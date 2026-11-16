package com.anode.autoconfiguration.gitlab;

import static com.anode.autoconfiguration.gitlab.GitLabProperties.CONFIG_PREFIX;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** GitLab settings; the token comes from the environment (e.g. ANODE_GITLAB_TOKEN). */
@ConfigurationProperties(CONFIG_PREFIX)
public class GitLabProperties {
    public static final String CONFIG_PREFIX = "anode.gitlab";

    /** Enable the GitLab auto-configuration. */
    private boolean enabled = true;
    /** Instance URL. */
    private String url = "https://gitlab.com";
    /** Personal, project or group access token. */
    private String token;
    private final Webhook webhook = new Webhook();

    /** Webhook endpoint, created when a secret token is set. */
    public static class Webhook {
        /** Secret token configured on the GitLab webhook (sent as X-Gitlab-Token). */
        private String secret;
        /** Path of the endpoint (must be reachable without user authentication). */
        private String path = "/webhooks/gitlab";

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

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Webhook getWebhook() {
        return webhook;
    }
}
