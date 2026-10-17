package com.anode.autoconfiguration.notion;

import static com.anode.autoconfiguration.notion.NotionProperties.CONFIG_PREFIX;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Notion integration settings; the token comes from the environment (e.g. ANODE_NOTION_TOKEN). */
@ConfigurationProperties(CONFIG_PREFIX)
public class NotionProperties {
    public static final String CONFIG_PREFIX = "anode.notion";

    /** Enable the Notion auto-configuration. */
    private boolean enabled = true;
    /** Internal integration token. */
    private String token;
    /** API root. */
    private String baseUrl = "https://api.notion.com/v1";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
