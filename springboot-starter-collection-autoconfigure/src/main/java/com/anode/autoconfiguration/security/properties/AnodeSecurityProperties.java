package com.anode.autoconfiguration.security.properties;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.anode.autoconfiguration.security.properties.AnodeSecurityProperties.CONFIG_PREFIX;

@ConfigurationProperties(CONFIG_PREFIX)
public class AnodeSecurityProperties {
    public static final String CONFIG_PREFIX = "anode.security";

    private boolean enabled = true;
    private boolean envLocalJwt = false;
    private boolean envOkta = false;
    private boolean envKerberos = false;
    private boolean envCognito = false;
    /** Any OpenID Connect provider configured through {@code spring.security.oauth2.client.*}. */
    private boolean envOidc = false;
    private boolean mock = false;
    @Value("${POST_LOGOUT_URL:${FRONTEND_URL:http://localhost:5173}}")
    private String postLogoutUrl;
    @Value("${LOGIN_URL:${FRONTEND_URL:/login}}")
    private String loginUrl;
    private List<String> allowedPatterns = new ArrayList<>();
    private final Oidc oidc = new Oidc();
    private final Roles roles = new Roles();

    /** Generic OpenID Connect login. */
    public static class Oidc {
        /** Registration id under {@code spring.security.oauth2.client.registration}. */
        private String registrationId = "oidc";

        public String getRegistrationId() {
            return registrationId;
        }

        public void setRegistrationId(String registrationId) {
            this.registrationId = registrationId;
        }
    }

    /**
     * Roles from identity-provider groups. When {@link #mapping} is empty the historical claim
     * mapping ({@code role}/{@code Title} claims) applies.
     */
    public static class Roles {
        /** Claim holding the user's groups. */
        private String claim = "groups";
        /** Role (e.g. ROLE_ADMIN) to the groups granting it. */
        private Map<String, List<String>> mapping = new LinkedHashMap<>();

        public String getClaim() {
            return claim;
        }

        public void setClaim(String claim) {
            this.claim = claim;
        }

        public Map<String, List<String>> getMapping() {
            return mapping;
        }

        public void setMapping(Map<String, List<String>> mapping) {
            this.mapping = mapping;
        }
    }

    public Oidc getOidc() {
        return oidc;
    }

    public Roles getRoles() {
        return roles;
    }

    public boolean isEnvOidc() {
        return envOidc;
    }

    public void setEnvOidc(boolean envOidc) {
        this.envOidc = envOidc;
    }

    /** Whether users log in through the browser and then hold a session cookie. */
    public boolean isSessionLogin() {
        return mock || envOkta || envCognito || envOidc;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnvLocalJwt() {
        return envLocalJwt;
    }

    public void setEnvLocalJwt(boolean envLocalJwt) {
        this.envLocalJwt = envLocalJwt;
    }

    public boolean isEnvOkta() {
        return envOkta;
    }

    public void setEnvOkta(boolean envOkta) {
        this.envOkta = envOkta;
    }

    public boolean isEnvKerberos() {
        return envKerberos;
    }

    public void setEnvKerberos(boolean envKerberos) {
        this.envKerberos = envKerberos;
    }

    public boolean isEnvCognito() {
        return envCognito;
    }

    public void setEnvCognito(boolean envCognito) {
        this.envCognito = envCognito;
    }

    public boolean isMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }

    public String getPostLogoutUrl() {
        return postLogoutUrl;
    }

    public void setPostLogoutUrl(String postLogoutUrl) {
        this.postLogoutUrl = postLogoutUrl;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public List<String> getAllowedPatterns() {
        return allowedPatterns;
    }

    public void setAllowedPatterns(List<String> allowedPatterns) {
        this.allowedPatterns = allowedPatterns;
    }
}
