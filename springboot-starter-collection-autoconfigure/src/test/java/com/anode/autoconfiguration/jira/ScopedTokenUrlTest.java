package com.anode.autoconfiguration.jira;

import static org.assertj.core.api.Assertions.assertThat;

import com.anode.autoconfiguration.confluence.ConfluenceProperties;
import org.junit.jupiter.api.Test;

/** Scoped API tokens only work through the Atlassian gateway, with the site's cloud id. */
class ScopedTokenUrlTest {

    @Test
    void theSiteWithoutACloudIdTheGatewayWithOne() {
        var jira = new JiraProperties();
        jira.setBaseUrl("https://acme.atlassian.net/");
        assertThat(jira.url()).isEqualTo("https://acme.atlassian.net");
        jira.setCloudId(" 1234-abcd ");
        assertThat(jira.url()).isEqualTo("https://api.atlassian.com/ex/jira/1234-abcd");
        jira.setGatewayUrl("http://localhost:9999/");
        assertThat(jira.url()).isEqualTo("http://localhost:9999/ex/jira/1234-abcd");

        var confluence = new ConfluenceProperties();
        confluence.setBaseUrl("https://acme.atlassian.net");
        confluence.setCloudId("1234-abcd");
        assertThat(confluence.url()).isEqualTo("https://api.atlassian.com/ex/confluence/1234-abcd");
    }
}
