package com.anode.autoconfiguration.security.properties;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

import static com.anode.autoconfiguration.security.properties.AnodeSecurityProperties.CONFIG_PREFIX;

@ConfigurationProperties(CONFIG_PREFIX)
public class AnodeSecurityProperties {
    public static final String CONFIG_PREFIX = "anode.security";

    private boolean enabled = true;
    private boolean mock = false;
    @Value("${POST_LOGOUT_URL:${FRONTEND_URL:http://localhost:5173}}")
    private String postLogoutUrl;
    private List<String> allowedPatterns = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public List<String> getAllowedPatterns() {
        return allowedPatterns;
    }

    public void setAllowedPatterns(List<String> allowedPatterns) {
        this.allowedPatterns = allowedPatterns;
    }
}
