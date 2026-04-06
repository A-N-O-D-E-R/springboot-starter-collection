package com.anode.autoconfiguration.logging;

import com.anode.autoconfiguration.logging.properties.LoggingProperties;
import com.anode.logging.EventLoggingProperties;
import com.anode.logging.EventMarkers;
import com.anode.logging.service.ArchiveScheduler;
import com.anode.logging.service.ArchiveService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

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

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "logging.collection.archive",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public ArchiveService archiveService(LoggingProperties properties) {
        return new ArchiveService(
                properties.getArchive().getLogDirectory(),
                properties.getArchive().getArchiveAfterDays()
        );
    }

    @Configuration
    @ConditionalOnProperty(
        prefix = "logging.collection.archive",
        name = "scheduled",
        havingValue = "true",
        matchIfMissing = true
    )
    @EnableScheduling
    static class SchedulingConfiguration {

        @Bean
        @ConditionalOnMissingBean
        @ConditionalOnBean(ArchiveService.class)
        public ArchiveScheduler archiveScheduler(ArchiveService service) {
            return new ArchiveScheduler(service);
        }
    }
}
