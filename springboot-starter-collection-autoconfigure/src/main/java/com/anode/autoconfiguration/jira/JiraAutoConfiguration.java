package com.anode.autoconfiguration.jira;

import com.anode.jira.JiraClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.retry.RetryPolicy;

/** A {@link JiraClient} from {@code anode.jira.*}. */
@AutoConfiguration
@ConditionalOnClass(JiraClient.class)
@ConditionalOnProperty(prefix = JiraProperties.CONFIG_PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(JiraProperties.class)
public class JiraAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = JiraProperties.CONFIG_PREFIX, name = {"base-url", "email", "api-token"})
    JiraClient jiraClient(JiraProperties properties) {
        var retry = properties.getRetry();
        var policy = RetryPolicy.builder()
                .maxRetries(retry.getMaxRetries())
                .delay(retry.getDelay())
                .multiplier(2)
                .maxDelay(retry.getMaxDelay())
                .build();
        return new JiraClient(properties.getBaseUrl(), properties.getEmail(), properties.getApiToken(), policy);
    }
}
