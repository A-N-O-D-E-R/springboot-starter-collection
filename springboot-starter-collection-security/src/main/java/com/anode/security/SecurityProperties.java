package com.anode.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for the security starter.
 *
 * <p>Supports three authentication modes:
 * <ul>
 *   <li>{@code USERNAME_PASSWORD} (default) – form login and HTTP basic with an in-memory user store</li>
 *   <li>{@code OAUTH2} – OAuth 2.0 / OpenID Connect login delegated to Spring Security OAuth2</li>
 *   <li>{@code KERBEROS} – SPNEGO / Kerberos single-sign-on (requires spring-security-kerberos on the classpath)</li>
 * </ul>
 *
 * <pre>{@code
 * # application.yml example
 * anode:
 *   security:
 *     type: USERNAME_PASSWORD
 *     username-password:
 *       users:
 *         - username: admin
 *           password: "{bcrypt}$2a$10$..."
 *           roles: [ADMIN, USER]
 * }</pre>
 */
@ConfigurationProperties(prefix = "anode.security")
@Getter
@Setter 
public class SecurityProperties {

    private AuthType type = AuthType.USERNAME_PASSWORD;
    private KerberosProperties kerberos = new KerberosProperties();
    private UsernamePasswordProperties usernamePassword = new UsernamePasswordProperties();

    public enum AuthType {
        OAUTH2,
        KERBEROS,
        USERNAME_PASSWORD
    }

    public record KerberosProperties(
        String servicePrincipal,
        String keytabLocation,
        String ldapServerUrl,
        String ldapSearchBase,
        String ldapSearchFilter
    ) {
        public KerberosProperties {
            if (ldapSearchFilter == null || ldapSearchFilter.isBlank()) {
                ldapSearchFilter = "(userPrincipalName={0})";
            }
        }
    }


    public static class UsernamePasswordProperties {
        private List<User> users = new ArrayList<>();

        @Getter
        @Setter
        public static class User {
            private String username;
            private String password;
            private List<String> roles = new ArrayList<>();
        }
    }
}
