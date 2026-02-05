package com.anode.autoconfiguration.logging;

import com.anode.logging.EventLoggingProperties;
import com.anode.logging.EventMarkers;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for structured event logging.
 * Enables the event logging properties for use in logback-spring.xml.
 *
 * <p>Logback configuration is handled via the included logback-spring.xml fragment.
 * No programmatic configuration of Logback is performed.</p>
 */
@Configuration
@ConditionalOnClass(EventMarkers.class)
@EnableConfigurationProperties(EventLoggingProperties.class)
public class EventLoggingAutoConfiguration {
}
