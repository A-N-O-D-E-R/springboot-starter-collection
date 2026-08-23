package com.anode.autoconfiguration.security;

import com.anode.autoconfiguration.security.properties.AnodeSecurityProperties;
import com.anode.security.oidc.AnodeClaimsToRoleMapper;
import com.anode.security.oidc.AnodeGrantedAuthoritiesMapper;
import com.anode.security.oidc.ClaimsToRoleMapper;
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
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.kerberos.authentication.KerberosServiceAuthenticationProvider;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.kerberos.web.authentication.SpnegoEntryPoint;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.anode.security.oidc.OidcUtils.getOidcUserInfo;
import static com.anode.security.oidc.OidcUtils.oidcAuthentication;
import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Single security auto-configuration for every supported mechanism: OIDC (Okta, Cognito),
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
@ConditionalOnExpression("${anode.security.mock:false} or ${anode.security.env-okta:false} or ${anode.security.env-cognito:false} or ${anode.security.env-local-jwt:false} or ${anode.security.env-kerberos:false}")
@ConditionalOnDefaultWebSecurity
@AutoConfigureOrder(-1) // run before Okta's own starter so it doesn't configure its own SecurityFilterChain
public class AnodeSecurityAutoConfiguration {

    @Bean
    public SecurityFilterChain configure(HttpSecurity http, AnodeSecurityProperties anodeSecurityProperties,
                                          ClaimsToRoleMapper claimsToRoleMapper,
                                          List<Customizer<HttpSecurity>> httpSecurityCustomizers,
                                          Optional<ClientRegistrationRepository> clientRegistrationRepository,
                                          Optional<AuthenticationManagerResolver<HttpServletRequest>> authenticationManagerResolver,
                                          Optional<SpnegoAuthenticationProcessingFilter> spnegoFilter,
                                          Optional<SpnegoEntryPoint> spnegoEntryPoint) throws Exception {

        var allowedPatterns = Stream.concat(
                Stream.of("/actuator/**"),
                anodeSecurityProperties.getAllowedPatterns().stream())
                .toArray(String[]::new);

        http.securityMatcher("/**")
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(allowedPatterns).permitAll()
                        .anyRequest().authenticated())
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable);

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
            } else if (anodeSecurityProperties.isEnvCognito()) {
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
    ClaimsToRoleMapper claimsToRoleMapper() {
        return new AnodeClaimsToRoleMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(value = "anode.security.env-local-jwt", havingValue = "true")
    JwtDecoder localJwtDecoder(Environment environment) {
        var secretKey = localJwtSecretKey(environment);
        var decoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(localJwtIssuer(environment)));
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
    @ConditionalOnExpression("!${anode.security.mock:false} and (${anode.security.env-okta:false} or ${anode.security.env-cognito:false} or ${anode.security.env-local-jwt:false})")
    AuthenticationManagerResolver<HttpServletRequest> authenticationManagerResolver(
            Environment environment, AnodeSecurityProperties anodeSecurityProperties, Optional<JwtDecoder> localJwtDecoder) {

        Map<String, AuthenticationManager> managersByIssuer = new LinkedHashMap<>();

        if (anodeSecurityProperties.isEnvOkta()) {
            var issuer = environment.getRequiredProperty("okta.oauth2.issuer");
            managersByIssuer.put(issuer, jwtAuthenticationManager(JwtDecoders.fromIssuerLocation(issuer)));
        }
        if (anodeSecurityProperties.isEnvCognito()) {
            var issuer = environment.getRequiredProperty("spring.security.oauth2.client.provider.cognito.issuer-uri");
            managersByIssuer.put(issuer, jwtAuthenticationManager(JwtDecoders.fromIssuerLocation(issuer)));
        }
        if (anodeSecurityProperties.isEnvLocalJwt()) {
            managersByIssuer.put(localJwtIssuer(environment), jwtAuthenticationManager(localJwtDecoder.orElseThrow()));
        }

        return new JwtIssuerAuthenticationManagerResolver(managersByIssuer::get);
    }

    private static AuthenticationManager jwtAuthenticationManager(JwtDecoder decoder) {
        return new ProviderManager(new JwtAuthenticationProvider(decoder));
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
