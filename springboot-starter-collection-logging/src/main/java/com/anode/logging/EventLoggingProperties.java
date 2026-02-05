package com.anode.logging;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for structured event logging.
 * These properties can be set via application.properties or application.yml.
 *
 * <pre>
 * logging.event.path=/var/log/myapp
 * logging.event.retention-days=30
 * </pre>
 */
@ConfigurationProperties(prefix = "logging.event")
public class EventLoggingProperties {

    /**
     * Directory path where event log files are written.
     * Defaults to the current directory if not specified.
     */
    private String path = ".";

    /**
     * Number of days to retain rotated event log files.
     * Defaults to 30 days.
     */
    private int retentionDays = 30;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }
}
