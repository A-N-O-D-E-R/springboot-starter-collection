package com.anode.autoconfiguration.gitlab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anode.gitlab.GitLabWebhookEvent;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.WebHookManager;
import org.gitlab4j.api.webhook.MergeRequestEvent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class GitLabAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GitLabAutoConfiguration.class));

    @Test
    void noClientWithoutToken() {
        runner.run(context -> assertThat(context).doesNotHaveBean(GitLabApi.class));
    }

    @Test
    void theClientTalksToTheConfiguredInstanceWithTheToken() throws Exception {
        AtomicReference<String> privateToken = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v4/projects/", exchange -> {
            privateToken.set(exchange.getRequestHeaders().getFirst("PRIVATE-TOKEN"));
            byte[] json = "{\"id\":9,\"path_with_namespace\":\"acme/api\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.length);
            exchange.getResponseBody().write(json);
            exchange.close();
        });
        server.start();
        try {
            runner.withPropertyValues("anode.gitlab.url=http://127.0.0.1:" + server.getAddress().getPort(),
                            "anode.gitlab.token=glpat-test")
                    .run(context -> {
                        var project = context.getBean(GitLabApi.class).getProjectApi().getProject("acme/api");
                        assertThat(project.getPathWithNamespace()).isEqualTo("acme/api");
                    });
            assertThat(privateToken.get()).isEqualTo("glpat-test");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void theWebhookEndpointExistsOnlyWithASecret() {
        var web = new WebApplicationContextRunner().withConfiguration(AutoConfigurations.of(GitLabAutoConfiguration.class));
        web.run(context -> assertThat(context).doesNotHaveBean(GitLabWebhookController.class));
        web.withPropertyValues("anode.gitlab.webhook.secret=s3cret")
                .run(context -> assertThat(context).hasSingleBean(GitLabWebhookController.class));
    }

    @Test
    void deliveriesWithTheSecretArePublishedOthersRejected() throws Exception {
        List<Object> published = new ArrayList<>();
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new GitLabWebhookController(new WebHookManager("s3cret"), published::add)).build();
        String body = "{\"object_kind\":\"merge_request\",\"object_attributes\":{\"iid\":3,\"action\":\"update\"}}";

        mvc.perform(post("/webhooks/gitlab").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("X-Gitlab-Token", "s3cret")
                        .header("X-Gitlab-Event", "Merge Request Hook")
                        .header("X-Gitlab-Event-UUID", "u-1"))
                .andExpect(status().isAccepted());
        mvc.perform(post("/webhooks/gitlab").contentType(MediaType.APPLICATION_JSON).content(body)
                        .header("X-Gitlab-Token", "wrong")
                        .header("X-Gitlab-Event", "Merge Request Hook"))
                .andExpect(status().is4xxClientError());

        assertThat(published).hasSize(1);
        var event = (GitLabWebhookEvent) published.getFirst();
        assertThat(event.uuid()).isEqualTo("u-1");
        assertThat(event.event()).isInstanceOfSatisfying(MergeRequestEvent.class,
                mr -> assertThat(mr.getObjectAttributes().getIid()).isEqualTo(3L));
    }
}
