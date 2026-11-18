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
    /**
     * The site's cloud id, for scoped API tokens: Atlassian only accepts them through its gateway
     * ({@code <gateway-url>/ex/jira/<cloud-id>}); unscoped tokens call the site directly. Empty: the site.
     */
    private String cloudId;
    /** Atlassian API gateway, used with {@code cloud-id}. */
    private String gatewayUrl = "https://api.atlassian.com";
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

    public String getCloudId() { return cloudId; }

    public void setCloudId(String cloudId) { this.cloudId = cloudId; }

    public String getGatewayUrl() { return gatewayUrl; }

    public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }

    /** Where Jira is called: the site, or the gateway when a cloud id is set. No trailing slash. */
    public String url() {
        String url = cloudId == null || cloudId.isBlank() ? baseUrl : gatewayUrl.replaceAll("/+$", "") + "/ex/jira/" + cloudId.strip();
        return url.replaceAll("/+$", "");
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
