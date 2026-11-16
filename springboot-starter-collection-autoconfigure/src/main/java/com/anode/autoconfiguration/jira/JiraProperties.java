package com.anode.autoconfiguration.jira;

import static com.anode.autoconfiguration.jira.JiraProperties.CONFIG_PREFIX;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Jira Cloud settings; the API token comes from the environment (e.g. ANODE_JIRA_API_TOKEN). */
@ConfigurationProperties(CONFIG_PREFIX)
public class JiraProperties {
    public static final String CONFIG_PREFIX = "anode.jira";

    /** Enable the Jira auto-configuration. */
    private boolean enabled = true;
    /** Site URL, e.g. https://your-domain.atlassian.net. */
    private String baseUrl;
    /** Account the API token belongs to. */
    private String email;
    /** API token of that account. */
    private String apiToken;
    private final Retry retry = new Retry();

    /** Retries of rate-limited (429) and unavailable (502/503/504) responses. */
    public static class Retry {
        private long maxRetries = 3;
        private Duration delay = Duration.ofSeconds(1);
        private Duration maxDelay = Duration.ofSeconds(10);

        public long getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(long maxRetries) {
            this.maxRetries = maxRetries;
        }

        public Duration getDelay() {
            return delay;
        }

        public void setDelay(Duration delay) {
            this.delay = delay;
        }

        public Duration getMaxDelay() {
            return maxDelay;
        }

        public void setMaxDelay(Duration maxDelay) {
            this.maxDelay = maxDelay;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public Retry getRetry() {
        return retry;
    }
}
