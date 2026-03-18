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
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Auto-configuration for application security.
 *
 * <p>Activates one of three authentication modes based on {@code anode.security.type}:
 * <ul>
 *   <li>{@code USERNAME_PASSWORD} (default) – form login + HTTP basic with optional in-memory users</li>
 *   <li>{@code OAUTH2} – delegates to Spring Security OAuth2 / OIDC login</li>
 *   <li>{@code KERBEROS} – SPNEGO/Kerberos SSO, handled by {@link KerberosSecurityAutoConfiguration}</li>
 * </ul>
 */
@Configuration
@ConditionalOnClass(SecurityFilterChain.class)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAutoConfiguration.class);

    // -------------------------------------------------------------------------
    // Username / password
    // -------------------------------------------------------------------------

    @Configuration
    @ConditionalOnProperty(name = "anode.security.type", havingValue = "USERNAME_PASSWORD", matchIfMissing = true)
    static class UsernamePasswordSecurityConfiguration {

        @Bean
        @ConditionalOnMissingBean(SecurityFilterChain.class)
        public SecurityFilterChain usernamePasswordFilterChain(HttpSecurity http) throws Exception {
            logger.info("Configuring username-password security (form login + HTTP basic)");
            return http
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .formLogin(Customizer.withDefaults())
                    .httpBasic(Customizer.withDefaults())
                    .build();
        }

        @Bean
        @ConditionalOnMissingBean(UserDetailsService.class)
        public UserDetailsService inMemoryUserDetailsService(SecurityProperties props) {
            var users = props.getUsernamePassword().getUsers();
            if (users.isEmpty()) {
                logger.warn("No users configured under anode.security.username-password.users. "
                        + "Falling back to Spring Boot's auto-generated user.");
                return new InMemoryUserDetailsManager();
            }
            var userDetails = users.stream()
                    .map(u -> {
                        logger.info("Registering in-memory user '{}' with roles {}", u.getUsername(), u.getRoles());
                        return User.withUsername(u.getUsername())
                                .password(u.getPassword())
                                .roles(u.getRoles().toArray(String[]::new))
                                .build();
                    })
                    .toList();
            return new InMemoryUserDetailsManager(userDetails);
        }

        @Bean
        @ConditionalOnMissingBean(PasswordEncoder.class)
        public PasswordEncoder passwordEncoder() {
            return PasswordEncoderFactories.createDelegatingPasswordEncoder();
        }
    }

    // -------------------------------------------------------------------------
    // OAuth2 / OIDC
    // -------------------------------------------------------------------------

    @Configuration
    @ConditionalOnProperty(name = "anode.security.type", havingValue = "OAUTH2")
    static class OAuth2SecurityConfiguration {

        @Bean
        @ConditionalOnMissingBean(SecurityFilterChain.class)
        public SecurityFilterChain oauth2FilterChain(HttpSecurity http) throws Exception {
            logger.info("Configuring OAuth2/OIDC security");
            return http
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .oauth2Login(Customizer.withDefaults())
                    .build();
        }
    }
}
