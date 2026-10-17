package com.anode.autoconfiguration.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anode.github.GitHubApp;
import com.anode.github.GitHubWebhookEvent;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GitHubAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GitHubAutoConfiguration.class));

    static String pkcs8Pem() throws Exception {
        var keys = KeyPairGenerator.getInstance("RSA");
        keys.initialize(2048);
        return "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(keys.generateKeyPair().getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
    }

    @Test
    void noAppWithoutCredentials() {
        runner.run(context -> assertThat(context).doesNotHaveBean(GitHubApp.class));
    }

    @Test
    void anAppFromProperties() throws Exception {
        runner.withPropertyValues("anode.github.app-id=1", "anode.github.private-key=" + pkcs8Pem())
                .run(context -> assertThat(context).hasSingleBean(GitHubApp.class));
    }

    @Test
    void canBeSwitchedOff() throws Exception {
        runner.withPropertyValues("anode.github.enabled=false", "anode.github.app-id=1", "anode.github.private-key=" + pkcs8Pem())
                .run(context -> assertThat(context).doesNotHaveBean(GitHubApp.class));
    }

    @Test
    void aWrongKeyFailsAtStartup() {
        runner.withPropertyValues("anode.github.app-id=1", "anode.github.private-key=not a key")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void theWebhookEndpointExistsOnlyWithASecret() {
        var web = new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(GitHubAutoConfiguration.class));
        web.run(context -> assertThat(context).doesNotHaveBean(GitHubWebhookController.class));
        web.withPropertyValues("anode.github.webhook.secret=s3cret")
                .run(context -> assertThat(context).hasSingleBean(GitHubWebhookController.class));
    }

    @Test
    void verifiedDeliveriesArePublishedOthersRejected() throws Exception {
        List<Object> published = new ArrayList<>();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new GitHubWebhookController("s3cret", published::add)).build();
        byte[] body = "{\"action\":\"submitted\"}".getBytes(StandardCharsets.UTF_8);
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("s3cret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));

        mvc.perform(post("/webhooks/github").content(body)
                        .header("X-Hub-Signature-256", signature)
                        .header("X-GitHub-Event", "pull_request_review")
                        .header("X-GitHub-Delivery", "d-1"))
                .andExpect(status().isAccepted());
        mvc.perform(post("/webhooks/github").content(body)
                        .header("X-Hub-Signature-256", "sha256=00")
                        .header("X-GitHub-Event", "pull_request_review")
                        .header("X-GitHub-Delivery", "d-2"))
                .andExpect(status().isUnauthorized());

        assertThat(published).containsExactly(
                new GitHubWebhookEvent("pull_request_review", "d-1", "{\"action\":\"submitted\"}"));
    }
}
