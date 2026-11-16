package com.anode.autoconfiguration.github;

import com.anode.github.GitHubApp;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** {@link GitHubApp} from {@code anode.github.*}, and the webhook endpoint when a secret is set. */
@AutoConfiguration
@ConditionalOnClass(GitHubApp.class)
@ConditionalOnProperty(prefix = GitHubProperties.CONFIG_PREFIX, name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GitHubProperties.class)
public class GitHubAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = GitHubProperties.CONFIG_PREFIX, name = {"app-id", "private-key"})
    GitHubApp gitHubApp(GitHubProperties properties) {
        return new GitHubApp(properties.getApiUrl(), properties.getAppId(), properties.getPrivateKey());
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
    @ConditionalOnProperty(prefix = GitHubProperties.CONFIG_PREFIX, name = "webhook.secret")
    static class Webhook {
        @Bean
        GitHubWebhookController gitHubWebhookController(GitHubProperties properties, ApplicationEventPublisher events) {
            return new GitHubWebhookController(properties.getWebhook().getSecret(), events);
        }
    }
}
