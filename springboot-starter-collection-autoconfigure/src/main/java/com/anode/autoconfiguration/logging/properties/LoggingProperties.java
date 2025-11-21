package com.anode.autoconfiguration.logging.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * Configuration properties for logging.
 */
@ConfigurationProperties(LoggingProperties.CONFIG_PREFIX)
public class LoggingProperties {
    public static final String CONFIG_PREFIX = "logging.collection";

    @NestedConfigurationProperty
    private LoggingFilterProperties filter = new LoggingFilterProperties();

    public LoggingFilterProperties getFilter() {
        return filter;
    }

    public void setFilter(LoggingFilterProperties filter) {
        this.filter = filter;
    }
}
