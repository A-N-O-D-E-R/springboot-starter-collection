package com.anode.autoconfiguration.security;

import com.anode.autoconfiguration.security.properties.AnodeSecurityProperties;
import com.anode.security.AnodeLocalJWTEnvironmentPostProcessor;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.ConditionalOnDefaultWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static org.springframework.security.config.Customizer.withDefaults;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({EnableWebSecurity.class, AnodeLocalJWTEnvironmentPostProcessor.class})
@EnableConfigurationProperties({AnodeSecurityProperties.class})
@EnableMethodSecurity(securedEnabled = true)
@ConditionalOnProperty(value ="anode.security.env-local-jwt", havingValue = "true")
@ConditionalOnDefaultWebSecurity
@AutoConfigureOrder(-1)
public class AnodeLocalJWTSecurityAutoConfiguration {

    @Bean
    public JwtDecoder jwtDecoder(Environment environment) {
        var secret = environment.getProperty("spring.security.oauth2.resourceserver.jwt.secret", "dev-secret-key");
        var keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        var secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder(Environment environment) {
        var secret = environment.getProperty(
                "spring.security.oauth2.resourceserver.jwt.secret",
                "dev-secret-key"
        );
        var keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        var secretKey = new SecretKeySpec(keyBytes, "HmacSHA256");

        return NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public SecurityFilterChain configure(
            HttpSecurity http,
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
                .formLogin(form -> form
                        .loginPage(anodeSecurityProperties.getLoginUrl())          // your custom page
                        .loginProcessingUrl("/api/users/login") // POST endpoint
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl(anodeSecurityProperties.getPostLogoutUrl())
                        .logoutSuccessUrl(anodeSecurityProperties.getLoginUrl())
                )
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));

        httpSecurityCustomizers.forEach(customizer -> customizer.customize(http));

        return http.build();
    }
}
