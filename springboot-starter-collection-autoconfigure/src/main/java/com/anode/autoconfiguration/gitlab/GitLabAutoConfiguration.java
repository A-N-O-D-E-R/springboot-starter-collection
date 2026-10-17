package com.anode.autoconfiguration.gitlab;

import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.WebHookManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** {@link GitLabApi} from {@code anode.gitlab.*}, and the webhook endpoint when a secret is set. */
@AutoConfiguration
@ConditionalOnClass(GitLabApi.class)
@ConditionalOnProperty(prefix = GitLabProperties.CONFIG_PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GitLabProperties.class)
public class GitLabAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = GitLabProperties.CONFIG_PREFIX, name = "token")
    GitLabApi gitLabApi(GitLabProperties properties) {
        return new GitLabApi(properties.getUrl(), properties.getToken());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    @ConditionalOnProperty(prefix = GitLabProperties.CONFIG_PREFIX, name = "webhook.secret")
    static class Webhook {
        @Bean
        GitLabWebhookController gitLabWebhookController(GitLabProperties properties, ApplicationEventPublisher events) {
            return new GitLabWebhookController(new WebHookManager(properties.getWebhook().getSecret()), events);
        }
    }
}
