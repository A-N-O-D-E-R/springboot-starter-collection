package com.anode.autoconfiguration.notion;

import notion.api.v1.NotionClient;
import notion.api.v1.logging.Slf4jLogger;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/** A {@link NotionClient} from {@code anode.notion.*}, logging through SLF4J. */
@AutoConfiguration
@ConditionalOnClass({NotionClient.class, Slf4jLogger.class})
@ConditionalOnProperty(prefix = NotionProperties.CONFIG_PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(NotionProperties.class)
public class NotionAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = NotionProperties.CONFIG_PREFIX, name = "token")
    NotionClient notionClient(NotionProperties properties) {
        var client = new NotionClient(properties.getToken());
        client.setBaseUrl(properties.getBaseUrl());
        client.setLogger(new Slf4jLogger());
        return client;
    }
}
