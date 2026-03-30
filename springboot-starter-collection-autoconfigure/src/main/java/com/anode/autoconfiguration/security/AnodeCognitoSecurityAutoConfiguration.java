package com.anode.autoconfiguration.security;

import com.anode.autoconfiguration.security.properties.AnodeSecurityProperties;
import com.anode.security.oidc.AnodeClaimsToRoleMapper;
import com.anode.security.oidc.AnodeGrantedAuthoritiesMapper;
import com.anode.security.oidc.ClaimsToRoleMapper;
import com.anode.security.oidc.OidcUtils;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.web.servlet.ConditionalOnDefaultWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;
import java.util.stream.Stream;

import static org.springframework.security.config.Customizer.withDefaults;
import static com.anode.security.oidc.OidcUtils.getOidcUserInfo;
import static com.anode.security.oidc.OidcUtils.oidcAuthentication;

@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass({EnableWebSecurity.class, OidcUtils.class})
@EnableConfigurationProperties({AnodeSecurityProperties.class})
@EnableMethodSecurity(securedEnabled = true)
@ConditionalOnProperty(value = "anode.security.env-cognito", havingValue = "true")
@ConditionalOnDefaultWebSecurity
@AutoConfigureOrder(-1)
public class AnodeCognitoSecurityAutoConfiguration {

    @Bean
    public SecurityFilterChain configure(HttpSecurity http, AnodeSecurityProperties anodeSecurityProperties, ClaimsToRoleMapper claimsToRoleMapper, List<Customizer<HttpSecurity>> httpSecurityCustomizers) throws Exception {

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
            http.oauth2Login(oauth2 -> oauth2
                    .userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(grantedAuthoritiesMapper(claimsToRoleMapper)))
                    .defaultSuccessUrl(anodeSecurityProperties.getPostLogoutUrl(), false))
                .oauth2Client(withDefaults())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
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
}
