package com.anode.autoconfiguration.security;

import com.anode.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.kerberos.authentication.KerberosAuthenticationProvider;
import org.springframework.security.kerberos.authentication.KerberosServiceAuthenticationProvider;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosClient;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.kerberos.web.authentication.SpnegoEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Auto-configuration for Kerberos/SPNEGO single-sign-on.
 *
 * <p>Only activated when:
 * <ol>
 *   <li>{@code anode.security.type=KERBEROS}</li>
 *   <li>{@code spring-security-kerberos-web} is present on the classpath</li>
 * </ol>
 *
 * <p>Required properties:
 * <ul>
 *   <li>{@code anode.security.kerberos.service-principal} – e.g. {@code HTTP/myapp.example.com@EXAMPLE.COM}</li>
 *   <li>{@code anode.security.kerberos.keytab-location} – filesystem path to the keytab file</li>
 * </ul>
 *
 * <p>A custom {@link org.springframework.security.core.userdetails.UserDetailsService} bean must be
 * provided by the application to resolve user details after SPNEGO token validation (e.g. via LDAP).
 */
@Configuration
@ConditionalOnClass(SpnegoAuthenticationProcessingFilter.class)
@ConditionalOnProperty(name = "anode.security.type", havingValue = "KERBEROS")
@EnableConfigurationProperties(SecurityProperties.class)
public class KerberosSecurityAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(KerberosSecurityAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain kerberosFilterChain(HttpSecurity http,
                                                   SpnegoEntryPoint spnegoEntryPoint,
                                                   KerberosServiceAuthenticationProvider kerberosServiceProvider,
                                                   KerberosAuthenticationProvider kerberosAuthProvider) throws Exception {
        logger.info("Configuring Kerberos/SPNEGO security");

        var authManager = http.getSharedObject(AuthenticationManagerBuilder.class)
                .authenticationProvider(kerberosServiceProvider)
                .authenticationProvider(kerberosAuthProvider)
                .build();

        var spnegoFilter = new SpnegoAuthenticationProcessingFilter();
        spnegoFilter.setAuthenticationManager(authManager);

        return http
                .exceptionHandling(ex -> ex.authenticationEntryPoint(spnegoEntryPoint))
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .addFilterBefore(spnegoFilter, BasicAuthenticationFilter.class)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public SpnegoEntryPoint spnegoEntryPoint() {
        return new SpnegoEntryPoint("/login");
    }

    @Bean
    @ConditionalOnMissingBean
    public KerberosAuthenticationProvider kerberosAuthenticationProvider() {
        var provider = new KerberosAuthenticationProvider();
        var client = new SunJaasKerberosClient();
        client.setDebug(logger.isDebugEnabled());
        provider.setKerberosClient(client);
        return provider;
    }

    @Bean
    @ConditionalOnMissingBean
    public SunJaasKerberosTicketValidator sunJaasKerberosTicketValidator(SecurityProperties props) {
        var kerberos = props.getKerberos();
        if (kerberos.getServicePrincipal() == null) {
            throw new IllegalStateException(
                    "Kerberos service principal not configured. Set anode.security.kerberos.service-principal");
        }
        if (kerberos.getKeytabLocation() == null) {
            throw new IllegalStateException(
                    "Kerberos keytab location not configured. Set anode.security.kerberos.keytab-location");
        }
        logger.info("Configuring Kerberos ticket validator for principal '{}'", kerberos.getServicePrincipal());
        var validator = new SunJaasKerberosTicketValidator();
        validator.setServicePrincipal(kerberos.getServicePrincipal());
        validator.setKeyTabLocation(new FileSystemResource(kerberos.getKeytabLocation()));
        validator.setDebug(logger.isDebugEnabled());
        return validator;
    }

    @Bean
    @ConditionalOnMissingBean
    public KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider(
            SunJaasKerberosTicketValidator ticketValidator) {
        var provider = new KerberosServiceAuthenticationProvider();
        provider.setTicketValidator(ticketValidator);
        return provider;
    }
}
