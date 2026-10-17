package com.anode.autoconfiguration.github;

import static com.anode.autoconfiguration.github.GitHubProperties.CONFIG_PREFIX;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** GitHub App settings; secrets come from the environment (e.g. ANODE_GITHUB_PRIVATE_KEY). */
@ConfigurationProperties(CONFIG_PREFIX)
public class GitHubProperties {
    public static final String CONFIG_PREFIX = "anode.github";

    /** Enable the GitHub auto-configuration. */
    private boolean enabled = true;
    /** REST API root: https://api.github.com, or https://HOST/api/v3 for GitHub Enterprise Server. */
    private String apiUrl = "https://api.github.com";
    /** GitHub App id. */
    private String appId;
    /** GitHub App private key, PKCS#8 PEM. */
    private String privateKey;
    private final Webhook webhook = new Webhook();

    /** Webhook endpoint, created when a secret is set. */
    public static class Webhook {
        /** Shared secret configured on the App's webhook. */
        private String secret;
        /** Path of the endpoint (must be reachable without user authentication). */
        private String path = "/webhooks/github";

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public Webhook getWebhook() {
        return webhook;
    }
}
