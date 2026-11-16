package com.anode.autoconfiguration.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;

/** The starter's security filter chain, through HTTP. */
class AnodeSecurityAutoConfigurationTest {

    static final String ISSUER = "https://factory.test";
    static final String SECRET = "0123456789abcdef0123456789abcdef";

    /** Bearer JWTs issued by the application itself (e.g. for its workers). */
    @Nested
    @SpringBootTest(classes = SecurityTestApplication.class, properties = {
            "anode.security.env-local-jwt=true",
            "anode.security.jwt.issuer=" + ISSUER,
            "spring.security.oauth2.resourceserver.jwt.secret=" + SECRET,
            "anode.security.roles.claim=groups",
            "anode.security.roles.mapping.ROLE_WORKER=workers",
            "anode.security.allowed-patterns=/public/**"
    })
    @AutoConfigureMockMvc
    @Import(LocalJwt.RevokedWorkers.class)
    class LocalJwt {

        /** An application-provided validator: here, worker "w-revoked" has been revoked. */
        @TestConfiguration
        static class RevokedWorkers {
            /** And an application URL rule: creating things is for workers only. */
            @Bean
            com.anode.security.AuthorizationRules workersOnly() {
                return rules -> rules.requestMatchers(org.springframework.http.HttpMethod.POST, "/things").hasRole("WORKER");
            }

            @Bean
            OAuth2TokenValidator<Jwt> notRevoked() {
                return jwt -> "w-revoked".equals(jwt.getSubject())
                        ? OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "revoked", null))
                        : OAuth2TokenValidatorResult.success();
            }
        }

        @Autowired MockMvc mvc;
        @Autowired JwtEncoder encoder;

        String token(String issuer, String subject, List<String> groups) {
            var claims = JwtClaimsSet.builder()
                    .issuer(issuer).subject(subject).claim("groups", groups)
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plus(5, ChronoUnit.MINUTES)).build();
            var header = JwsHeader.with(MacAlgorithm.HS256).build();
            return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        }

        @Test
        void aValidTokenAuthenticatesWithRolesMappedFromGroups() throws Exception {
            mvc.perform(get("/whoami").header("Authorization", "Bearer " + token(ISSUER, "w-1", List.of("workers"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("w-1"))
                    .andExpect(jsonPath("$.authorities", Matchers.hasItem("ROLE_WORKER")));
        }

        @Test
        void aBearerRequestNeedsNoCsrfToken() throws Exception {
            mvc.perform(post("/things").header("Authorization", "Bearer " + token(ISSUER, "w-1", List.of("workers"))))
                    .andExpect(status().isOk());
        }

        @Test
        void applicationUrlRulesApplyBeforeTheRequestIsHandled() throws Exception {
            mvc.perform(post("/things").header("Authorization", "Bearer " + token(ISSUER, "u-1", List.of("people"))))
                    .andExpect(status().isForbidden());
        }

        @Test
        void aTokenFromAnotherIssuerIsRejected() throws Exception {
            mvc.perform(get("/whoami").header("Authorization", "Bearer " + token("https://evil", "w-1", List.of())))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void applicationValidatorsAreApplied() throws Exception {
            mvc.perform(get("/whoami").header("Authorization", "Bearer " + token(ISSUER, "w-revoked", List.of("workers"))))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void allowedPatternsArePublic() throws Exception {
            mvc.perform(get("/public/ping")).andExpect(status().isOk());
        }

        @Test
        void anonymousRequestsAreRejected() throws Exception {
            mvc.perform(get("/whoami")).andExpect(status().isUnauthorized());
        }
    }

    /** Session login (here the mock form login): browser requests are protected against CSRF. */
    @Nested
    @SpringBootTest(classes = SecurityTestApplication.class, properties = {
            "anode.security.mock=true",
            "anode.security.roles.claim=groups",
            "anode.security.roles.mapping.ROLE_ADMIN=factory-admins"
    })
    @AutoConfigureMockMvc
    class SessionLogin {

        @Autowired MockMvc mvc;

        @Test
        void aStateChangingRequestWithoutCsrfTokenIsRejected() throws Exception {
            mvc.perform(post("/things").with(user("alice"))).andExpect(status().isForbidden());
        }

        @Test
        void aStateChangingRequestWithCsrfTokenIsAccepted() throws Exception {
            mvc.perform(post("/things").with(user("alice")).with(csrf())).andExpect(status().isOk());
        }

        @Test
        void mockUsersGetRolesFromTheirGroups() throws Exception {
            var session = mvc.perform(post("/login").with(csrf()).param("username", "alice").param("password", "x"))
                    .andReturn().getRequest().getSession();
            mvc.perform(get("/whoami").session((org.springframework.mock.web.MockHttpSession) session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authorities", Matchers.hasItem("ROLE_ADMIN")));
        }
    }

    /** Any OpenID Connect provider (Pocket ID, Keycloak...) configured through Spring's client properties. */
    @Nested
    @SpringBootTest(classes = SecurityTestApplication.class, properties = {
            "anode.security.env-oidc=true",
            "spring.security.oauth2.client.registration.oidc.client-id=factory",
            "spring.security.oauth2.client.registration.oidc.client-secret=secret",
            "spring.security.oauth2.client.registration.oidc.scope=openid,profile,email,groups",
            "spring.security.oauth2.client.registration.oidc.authorization-grant-type=authorization_code",
            "spring.security.oauth2.client.registration.oidc.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}",
            "spring.security.oauth2.client.provider.oidc.authorization-uri=https://idp.test/authorize",
            "spring.security.oauth2.client.provider.oidc.token-uri=https://idp.test/token",
            "spring.security.oauth2.client.provider.oidc.jwk-set-uri=https://idp.test/jwks",
            "spring.security.oauth2.client.provider.oidc.user-name-attribute=sub",
            "anode.security.allowed-patterns=/public/**"
    })
    @AutoConfigureMockMvc
    class GenericOidc {

        @Autowired MockMvc mvc;

        @Test
        void browsersAreSentToTheProvider() throws Exception {
            mvc.perform(get("/whoami").header("Accept", "text/html"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(header().string("Location", Matchers.endsWith("/oauth2/authorization/oidc")));
        }

        @Test
        void theStarterChainIsTheOneInPlace() throws Exception {
            mvc.perform(get("/public/ping")).andExpect(status().isOk());
        }
    }
}
