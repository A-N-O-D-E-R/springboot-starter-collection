package com.anode.autoconfiguration.jira;

import static org.assertj.core.api.Assertions.assertThat;

import com.anode.jira.JiraClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class JiraAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(JiraAutoConfiguration.class));

    @Test
    void aClientOnlyWithSiteAccountAndToken() {
        runner.withPropertyValues("anode.jira.base-url=https://acme.atlassian.net", "anode.jira.email=bot@acme.org")
                .run(context -> assertThat(context).doesNotHaveBean(JiraClient.class));
        runner.withPropertyValues("anode.jira.base-url=https://acme.atlassian.net", "anode.jira.email=bot@acme.org",
                        "anode.jira.api-token=t")
                .run(context -> assertThat(context).hasSingleBean(JiraClient.class));
    }

    @Test
    void canBeSwitchedOff() {
        runner.withPropertyValues("anode.jira.enabled=false", "anode.jira.base-url=https://acme.atlassian.net",
                        "anode.jira.email=bot@acme.org", "anode.jira.api-token=t")
                .run(context -> assertThat(context).doesNotHaveBean(JiraClient.class));
    }
}
