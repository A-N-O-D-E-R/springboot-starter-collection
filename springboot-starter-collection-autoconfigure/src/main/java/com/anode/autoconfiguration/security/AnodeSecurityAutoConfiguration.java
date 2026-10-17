package com.anode.autoconfiguration.security;

import com.anode.autoconfiguration.security.properties.AnodeSecurityProperties;
import com.anode.security.AuthorizationRules;
import com.anode.security.oidc.AnodeClaimsToRoleMapper;
import com.anode.security.oidc.AnodeGrantedAuthoritiesMapper;
import com.anode.security.oidc.ClaimsToRoleMapper;
import com.anode.security.oidc.GroupsClaimsToRoleMapper;
import com.anode.security.oidc.OidcUtils;
import com.okta.spring.boot.oauth.Okta;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.ConditionalOnDefaultWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.kerberos.authentication.KerberosServiceAuthenticationProvider;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.kerberos.web.authentication.SpnegoEntryPoint;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.anode.security.oidc.OidcUtils.getOidcUserInfo;
import static com.anode.security.oidc.OidcUtils.oidcAuthentication;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Single security auto-configuration for every supported mechanism: OIDC (Okta, Cognito, any
 * provider through {@code anode.security.env-oidc}),
 * local self-issued JWT, mock/form-login, and Kerberos/SPNEGO. Exactly one
 * {@link SecurityFilterChain} is built.
 * <p>
 * JWT-bearing requests (Okta/Cognito/local) are routed to the right {@link AuthenticationManager}
 * by issuer via a {@link JwtIssuerAuthenticationManagerResolver} — each provider, including the
 * local one, is just another trusted issuer. Kerberos runs as an independent
 * {@link SpnegoAuthenticationProcessingFilter}/{@link KerberosServiceAuthenticationProvider} pair;
 * both paths still deposit a standard Spring Security {@code Authentication} in the
 * {@code SecurityContext}. When Kerberos is combined with a browser login mode (Okta, Cognito,
 * mock), Spring Security's own content-negotiation split decides which challenge a given request
 * gets: browser navigations (Accept: text/html) fall through to the OIDC/mock login already
 * registered, everything else (SPNEGO-capable clients, API calls) gets the Kerberos
 * {@code WWW-Authenticate: Negotiate} challenge — see {@code defaultAuthenticationEntryPointFor}
 * below.
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({EnableWebSecurity.class, OidcUtils.class})
@EnableConfigurationProperties(AnodeSecurityProperties.class)
@EnableMethodSecurity(securedEnabled = true)
@ConditionalOnProperty(value = "anode.security.enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnExpression("${anode.security.mock:false} or ${anode.security.env-okta:false} or ${anode.security.env-cognito:false} or ${anode.security.env-oidc:false} or ${anode.security.env-local-jwt:false} or ${anode.security.env-kerberos:false}")
@ConditionalOnDefaultWebSecurity
@AutoConfigureOrder(-1) // run before Okta's own starter so it doesn't configure its own SecurityFilterChain
public class AnodeSecurityAutoConfiguration {

    @Bean
    public SecurityFilterChain configure(HttpSecurity http, AnodeSecurityProperties anodeSecurityProperties,
                                          ClaimsToRoleMapper claimsToRoleMapper,
                                          List<Customizer<HttpSecurity>> httpSecurityCustomizers,
                                          List<AuthorizationRules> authorizationRules,
                                          Optional<ClientRegistrationRepository> clientRegistrationRepository,
                                          Optional<AuthenticationManagerResolver<HttpServletRequest>> authenticationManagerResolver,
                                          Optional<SpnegoAuthenticationProcessingFilter> spnegoFilter,
                                          Optional<SpnegoEntryPoint> spnegoEntryPoint) throws Exception {

        var allowedPatterns = Stream.concat(
                Stream.of("/actuator/**"),
                anodeSecurityProperties.getAllowedPatterns().stream())
                .toArray(String[]::new);

        http.securityMatcher("/**")
                .authorizeHttpRequests(requests -> {
                    requests.requestMatchers(allowedPatterns).permitAll();
                    authorizationRules.forEach(rules -> rules.configure(requests));
                    requests.anyRequest().authenticated();
                })
                .cors(withDefaults());

        if (anodeSecurityProperties.isSessionLogin()) {
            // Browser sessions are cookie-authenticated: protect them against cross-site requests
            // (SPA flavour: token in the XSRF-TOKEN cookie, echoed in X-XSRF-TOKEN). Requests that
            // authenticate with a bearer token carry no ambient credential and are exempt.
            http.csrf(csrf -> csrf.spa().ignoringRequestMatchers(AnodeSecurityAutoConfiguration::hasBearerToken));
        } else {
            http.csrf(AbstractHttpConfigurer::disable);
        }

        if (anodeSecurityProperties.isMock()) {
            http.formLogin(withDefaults())
                    .logout(logout -> logout.logoutSuccessUrl(anodeSecurityProperties.getPostLogoutUrl()))
                    .authenticationManager(authentication -> switch (authentication.getPrincipal()) {
                        case String user -> oidcAuthentication(getOidcUserInfo(user), claimsToRoleMapper);
                        case null, default -> throw new InternalAuthenticationServiceException("Can not read user principal");
                    });
        } else {
            if (anodeSecurityProperties.isEnvOkta()) {
                Okta.configureOAuth2WithPkce(http, clientRegistrationRepository.orElseThrow());
                http.oauth2Client(withDefaults());
                if (anodeSecurityProperties.getLoginUrl() != null && !anodeSecurityProperties.getLoginUrl().isEmpty()) {
                    http.oauth2Login(oauth2 -> oauth2.loginPage(anodeSecurityProperties.getLoginUrl()));
                }
            } else if (anodeSecurityProperties.isEnvCognito() || anodeSecurityProperties.isEnvOidc()) {
                http.oauth2Login(oauth2 -> oauth2
                                .userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(grantedAuthoritiesMapper(claimsToRoleMapper))))
                        .oauth2Client(withDefaults());
            }
            authenticationManagerResolver.ifPresent(resolver ->
                    http.oauth2ResourceServer(oauth2 -> oauth2.authenticationManagerResolver(resolver)));
        }

        if (spnegoFilter.isPresent() && spnegoEntryPoint.isPresent()) {
            // Registered as a *default-for* entry, not the single default: if a browser login mode
            // (Okta/Cognito/mock) is also active above, its own entry point is scoped to
            // Accept: text/html requests and takes priority; this one catches everything else
            // (Negotiate-capable / API clients). If Kerberos is the only mechanism enabled, this
            // is the sole mapping and becomes the effective default outright.
            http.addFilterBefore(spnegoFilter.get(), BasicAuthenticationFilter.class)
                    .exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(spnegoEntryPoint.get(), AnyRequestMatcher.INSTANCE));
        }

        httpSecurityCustomizers.forEach(customizer -> customizer.customize(http));

        return http.build();
    }

    @Bean
    GrantedAuthoritiesMapper grantedAuthoritiesMapper(ClaimsToRoleMapper claimsToRoleMapper) {
        return new AnodeGrantedAuthoritiesMapper(claimsToRoleMapper);
    }

    @Bean
    @ConditionalOnMissingBean
    ClaimsToRoleMapper claimsToRoleMapper(AnodeSecurityProperties properties) {
        var roles = properties.getRoles();
        return roles.getMapping().isEmpty()
                ? new AnodeClaimsToRoleMapper()
                : new GroupsClaimsToRoleMapper(roles.getClaim(), roles.getMapping());
    }

    private static boolean hasBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        return authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "anode.security.env-local-jwt", havingValue = "true")
    JwtDecoder localJwtDecoder(Environment environment, List<OAuth2TokenValidator<Jwt>> applicationValidators) {
        var secretKey = localJwtSecretKey(environment);
        var decoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        // expiry, issuer, then the application's own checks (e.g. "this worker was revoked")
        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefaultWithIssuer(localJwtIssuer(environment)));
        validators.addAll(applicationValidators);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(validators));
        return decoder;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "anode.security.env-local-jwt", havingValue = "true")
    JwtEncoder localJwtEncoder(Environment environment) {
        return NimbusJwtEncoder.withSecretKey(localJwtSecretKey(environment)).algorithm(MacAlgorithm.HS256).build();
    }

    /**
     * Resolves the {@link AuthenticationManager} to use for a JWT-bearing request based on its
     * issuer claim, trusting Okta/Cognito (via OIDC discovery) and this app's own local-JWT
     * issuer — mirrors {@code AtsSecurityAutoconfiguration}, extended so the local issuer is just
     * another trusted entry rather than a separate mechanism. Not created in mock mode, nor when
     * none of Okta/Cognito/local-JWT are enabled (e.g. Kerberos-only setups have no JWT issuer to
     * resolve).
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnExpression("!${anode.security.mock:false} and (${anode.security.env-okta:false} or ${anode.security.env-cognito:false} or ${anode.security.env-oidc:false} or ${anode.security.env-local-jwt:false})")
    AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver(
            Environment environment, AnodeSecurityProperties anodeSecurityProperties, Optional<JwtDecoder> localJwtDecoder,
            ClaimsToRoleMapper claimsToRoleMapper) {

        Map<String, AuthenticationManager> managersByIssuer = new LinkedHashMap<>();

        if (anodeSecurityProperties.isEnvOkta()) {
            var issuer = environment.getRequiredProperty("okta.oauth2.issuer");
            managersByIssuer.put(issuer, jwtAuthenticationManager(JwtDecoders.fromIssuerLocation(issuer), claimsToRoleMapper));
        }
        if (anodeSecurityProperties.isEnvCognito()) {
            var issuer = environment.getRequiredProperty("spring.security.oauth2.client.provider.cognito.issuer-uri");
            managersByIssuer.put(issuer, jwtAuthenticationManager(JwtDecoders.fromIssuerLocation(issuer), claimsToRoleMapper));
        }
        if (anodeSecurityProperties.isEnvOidc()) {
            // bearer tokens of the OIDC provider are accepted when its issuer is known
            var issuer = environment.getProperty("spring.security.oauth2.client.provider.%s.issuer-uri"
                    .formatted(anodeSecurityProperties.getOidc().getRegistrationId()));
            if (issuer != null) {
                managersByIssuer.put(issuer, jwtAuthenticationManager(JwtDecoders.fromIssuerLocation(issuer), claimsToRoleMapper));
            }
        }
        if (anodeSecurityProperties.isEnvLocalJwt()) {
            managersByIssuer.put(localJwtIssuer(environment), jwtAuthenticationManager(localJwtDecoder.orElseThrow(), claimsToRoleMapper));
        }

        return new JwtIssuerAuthenticationManagerResolver(managersByIssuer::get);
    }

    /** Bearer JWTs get their scopes plus the roles the mapper derives from their claims. */
    private static AuthenticationManager jwtAuthenticationManager(JwtDecoder decoder, ClaimsToRoleMapper claimsToRoleMapper) {
        var scopes = new JwtGrantedAuthoritiesConverter();
        var converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>(scopes.convert(jwt));
            claimsToRoleMapper.map(jwt.getClaims()).forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));
            return authorities;
        });
        var provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(converter);
        return new ProviderManager(provider);
    }

    private static SecretKeySpec localJwtSecretKey(Environment environment) {
        var secret = environment.getRequiredProperty("spring.security.oauth2.resourceserver.jwt.secret");
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    private static String localJwtIssuer(Environment environment) {
        return environment.getRequiredProperty("anode.security.jwt.issuer");
    }

    // --- Kerberos/SPNEGO: independent of the JWT-issuer resolver above. A ticket is validated and
    // mapped to a standard Authentication via KerberosServiceAuthenticationProvider, exactly like
    // any other AuthenticationProvider. ---

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(SpnegoAuthenticationProcessingFilter.class)
    @ConditionalOnProperty(value = "anode.security.env-kerberos", havingValue = "true")
    SunJaasKerberosTicketValidator kerberosTicketValidator(Environment environment) {
        var validator = new SunJaasKerberosTicketValidator();
        validator.setServicePrincipal(environment.getRequiredProperty("spring.security.kerberos.service-principal"));
        validator.setKeyTabLocation(new FileSystemResource(
                environment.getRequiredProperty("spring.security.kerberos.keytab-location")));
        return validator;
    }

    /**
     * Default UserDetailsService for Kerberos: creates a user with the Kerberos principal as
     * username and ROLE_USER authority. Override this bean to load roles from LDAP or another source.
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(SpnegoAuthenticationProcessingFilter.class)
    @ConditionalOnProperty(value = "anode.security.env-kerberos", havingValue = "true")
    UserDetailsService kerberosUserDetailsService() {
        return username -> User.withUsername(username)
                .password("{noop}")
                .roles("USER")
                .build();
    }

    @Bean
    @ConditionalOnClass(SpnegoAuthenticationProcessingFilter.class)
    @ConditionalOnProperty(value = "anode.security.env-kerberos", havingValue = "true")
    KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider(
            SunJaasKerberosTicketValidator kerberosTicketValidator, UserDetailsService kerberosUserDetailsService) {
        var provider = new KerberosServiceAuthenticationProvider();
        provider.setTicketValidator(kerberosTicketValidator);
        provider.setUserDetailsService(kerberosUserDetailsService);
        return provider;
    }

    @Bean
    @ConditionalOnClass(SpnegoAuthenticationProcessingFilter.class)
    @ConditionalOnProperty(value = "anode.security.env-kerberos", havingValue = "true")
    SpnegoEntryPoint spnegoEntryPoint() {
        return new SpnegoEntryPoint();
    }

    @Bean
    @ConditionalOnClass(SpnegoAuthenticationProcessingFilter.class)
    @ConditionalOnProperty(value = "anode.security.env-kerberos", havingValue = "true")
    SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter(
            KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider) {
        var filter = new SpnegoAuthenticationProcessingFilter();
        filter.setAuthenticationManager(new ProviderManager(kerberosServiceAuthenticationProvider));
        return filter;
    }
}
