package com.anode.b2;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Backblaze B2 integration.
 */
@ConfigurationProperties(prefix = "b2")
public class B2Properties {

    /**
     * Enable B2 integration.
     */
    private boolean enabled = true;

    /**
     * B2 Application Key ID (formerly Account ID).
     */
    private String applicationKeyId;

    /**
     * B2 Application Key (secret).
     */
    private String applicationKey;

    /**
     * Default bucket name to use for operations.
     */
    private String defaultBucketName;

    /**
     * Connection timeout in seconds.
     */
    private int connectionTimeoutSeconds = 30;

    /**
     * Socket timeout in seconds.
     */
    private int socketTimeoutSeconds = 60;

    /**
     * Maximum number of retry attempts.
     */
    private int maxRetries = 3;

    /**
     * User agent to use for B2 API requests.
     */
    private String userAgent = "spring-boot-starter-b2/0.0.1";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApplicationKeyId() {
        return applicationKeyId;
    }

    public void setApplicationKeyId(String applicationKeyId) {
        this.applicationKeyId = applicationKeyId;
    }

    public String getApplicationKey() {
        return applicationKey;
    }

    public void setApplicationKey(String applicationKey) {
        this.applicationKey = applicationKey;
    }

    public String getDefaultBucketName() {
        return defaultBucketName;
    }

    public void setDefaultBucketName(String defaultBucketName) {
        this.defaultBucketName = defaultBucketName;
    }

    public int getConnectionTimeoutSeconds() {
        return connectionTimeoutSeconds;
    }

    public void setConnectionTimeoutSeconds(int connectionTimeoutSeconds) {
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
    }

    public int getSocketTimeoutSeconds() {
        return socketTimeoutSeconds;
    }

    public void setSocketTimeoutSeconds(int socketTimeoutSeconds) {
        this.socketTimeoutSeconds = socketTimeoutSeconds;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
