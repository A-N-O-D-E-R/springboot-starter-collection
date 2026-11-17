package com.anode.autoconfiguration.confluence;

import static org.assertj.core.api.Assertions.assertThat;

import com.anode.confluence.ConfluenceClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ConfluenceAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConfluenceAutoConfiguration.class));

    @Test
    void aClientOnlyWithSiteAccountAndToken() {
        runner.withPropertyValues("anode.confluence.base-url=https://acme.atlassian.net", "anode.confluence.email=bot@acme.org")
                .run(context -> assertThat(context).doesNotHaveBean(ConfluenceClient.class));
        runner.withPropertyValues("anode.confluence.base-url=https://acme.atlassian.net", "anode.confluence.email=bot@acme.org",
                        "anode.confluence.api-token=t")
                .run(context -> assertThat(context).hasSingleBean(ConfluenceClient.class));
    }

    @Test
    void canBeSwitchedOff() {
        runner.withPropertyValues("anode.confluence.enabled=false", "anode.confluence.base-url=https://acme.atlassian.net",
                        "anode.confluence.email=bot@acme.org", "anode.confluence.api-token=t")
                .run(context -> assertThat(context).doesNotHaveBean(ConfluenceClient.class));
    }
}
