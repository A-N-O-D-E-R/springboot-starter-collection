package com.anode.autoconfiguration.security.properties;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

import static com.anode.autoconfiguration.security.properties.AnodeSecurityProperties.CONFIG_PREFIX;

@ConfigurationProperties(CONFIG_PREFIX)
public class AnodeSecurityProperties {
    public static final String CONFIG_PREFIX = "anode.security";

    private boolean envLocalJwt = false;
    private boolean envOkta = false;
    private boolean envKerberos = false;
    private boolean envCognito = false;
    private boolean mock = false;
    @Value("${POST_LOGOUT_URL:${FRONTEND_URL:http://localhost:5173}}")
    private String postLogoutUrl;
    @Value("${LOGIN_URL:${FRONTEND_URL:/login}}")
    private String loginUrl;
    private List<String> allowedPatterns = new ArrayList<>();

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
