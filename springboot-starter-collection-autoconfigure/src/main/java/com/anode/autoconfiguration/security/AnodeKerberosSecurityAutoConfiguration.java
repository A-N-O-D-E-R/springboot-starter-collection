package com.anode.autoconfiguration.security;

import com.anode.autoconfiguration.security.properties.AnodeSecurityProperties;
import com.anode.security.oidc.AnodeClaimsToRoleMapper;
import com.anode.security.oidc.ClaimsToRoleMapper;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.ConditionalOnDefaultWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.kerberos.authentication.KerberosServiceAuthenticationProvider;
import org.springframework.security.kerberos.authentication.sun.SunJaasKerberosTicketValidator;
import org.springframework.security.kerberos.web.authentication.SpnegoAuthenticationProcessingFilter;
import org.springframework.security.kerberos.web.authentication.SpnegoEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import java.util.List;
import java.util.stream.Stream;

import static org.springframework.security.config.Customizer.withDefaults;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({EnableWebSecurity.class, SpnegoAuthenticationProcessingFilter.class})
@EnableConfigurationProperties({AnodeSecurityProperties.class})
@EnableMethodSecurity(securedEnabled = true)
@ConditionalOnProperty(value = "anode.security.env-kerberos", havingValue = "true")
@ConditionalOnDefaultWebSecurity
@AutoConfigureOrder(-1)
public class AnodeKerberosSecurityAutoConfiguration {

    @Bean
    public SunJaasKerberosTicketValidator sunJaasKerberosTicketValidator(Environment env) {
        var validator = new SunJaasKerberosTicketValidator();
        validator.setServicePrincipal(env.getRequiredProperty("spring.security.kerberos.service-principal"));
        validator.setKeyTabLocation(new FileSystemResource(
                env.getRequiredProperty("spring.security.kerberos.keytab-location")));
        return validator;
    }

    @Bean
    public KerberosServiceAuthenticationProvider kerberosServiceAuthenticationProvider(
            SunJaasKerberosTicketValidator ticketValidator,
            UserDetailsService userDetailsService) {
        var provider = new KerberosServiceAuthenticationProvider();
        provider.setTicketValidator(ticketValidator);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }

    @Bean
    public AuthenticationManager kerberosAuthenticationManager(
            KerberosServiceAuthenticationProvider kerberosProvider) {
        return new ProviderManager(List.of(kerberosProvider));
    }

    @Bean
    public SpnegoEntryPoint spnegoEntryPoint() {
        return new SpnegoEntryPoint();
    }

    @Bean
    public SpnegoAuthenticationProcessingFilter spnegoAuthenticationProcessingFilter(
            AuthenticationManager kerberosAuthenticationManager) {
        var filter = new SpnegoAuthenticationProcessingFilter();
        filter.setAuthenticationManager(kerberosAuthenticationManager);
        return filter;
    }

    @Bean
    public SecurityFilterChain configure(
            HttpSecurity http,
            SpnegoAuthenticationProcessingFilter spnegoFilter,
            SpnegoEntryPoint spnegoEntryPoint,
            AnodeSecurityProperties anodeSecurityProperties,
            List<Customizer<HttpSecurity>> httpSecurityCustomizers) throws Exception {

        var allowedPatterns = Stream.concat(
                Stream.of("/actuator/**"),
                anodeSecurityProperties.getAllowedPatterns().stream())
                .toArray(String[]::new);

        http.securityMatcher("/**")
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(allowedPatterns).permitAll()
                        .anyRequest().authenticated())
                .cors(withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(e -> e.authenticationEntryPoint(spnegoEntryPoint))
                .addFilterBefore(spnegoFilter, BasicAuthenticationFilter.class);

        httpSecurityCustomizers.forEach(customizer -> customizer.customize(http));

        return http.build();
    }

    /**
     * Default UserDetailsService for Kerberos: creates a user with the Kerberos principal as
     * username and ROLE_USER authority. Override this bean to load roles from LDAP or another source.
     */
    @Bean
    @ConditionalOnMissingBean
    public UserDetailsService kerberosUserDetailsService() {
        return username -> User.withUsername(username)
                .password("{noop}")
                .roles("USER")
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    ClaimsToRoleMapper claimsToRoleMapper() {
        return new AnodeClaimsToRoleMapper();
    }
}
