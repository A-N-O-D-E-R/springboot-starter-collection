package com.anode.autoconfiguration.confluence;

import com.anode.confluence.ConfluenceClient;
import com.anode.jira.TransientResponseRetry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/** A {@link ConfluenceClient} from {@code anode.confluence.*}, retrying 429/502/503/504 like the Jira client. */
@AutoConfiguration
@ConditionalOnClass(ConfluenceClient.class)
@ConditionalOnProperty(prefix = ConfluenceProperties.CONFIG_PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(ConfluenceProperties.class)
public class ConfluenceAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = ConfluenceProperties.CONFIG_PREFIX, name = {"base-url", "email", "api-token"})
    ConfluenceClient confluenceClient(ConfluenceProperties properties, ObjectProvider<RestClient.Builder> builders) {
        var retry = properties.getRetry();
        var policy = RetryPolicy.builder()
                .maxRetries(retry.getMaxRetries())
                .delay(retry.getDelay())
                .multiplier(2)
                .maxDelay(retry.getMaxDelay())
                .build();
        RestClient rest = builders.getIfAvailable(RestClient::builder).clone()
                .baseUrl(properties.url())
                .defaultHeaders(headers -> headers.setBasicAuth(properties.getEmail(), properties.getApiToken()))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(new TransientResponseRetry(policy))
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(rest)).build().createClient(ConfluenceClient.class);
    }
}
