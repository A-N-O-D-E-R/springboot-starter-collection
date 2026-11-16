package com.anode.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anode.jira.client.model.Comment;
import com.anode.jira.client.model.IssueTransition;
import com.anode.jira.client.model.IssueUpdateDetails;
import com.anode.jira.client.model.SearchAndReconcileRequestBean;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.web.client.HttpClientErrorException;

/** JiraClient against a local stand-in for Jira Cloud. */
class JiraClientTest {

    record Call(String method, String path, String query, String authorization, String body) {}

    record Reply(int status, String json) {}

    private HttpServer server;
    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private final Map<String, Deque<Reply>> replies = new java.util.concurrent.ConcurrentHashMap<>();
    private JiraClient jira;

    private void reply(String path, int status, String json) {
        replies.computeIfAbsent(path, p -> new ArrayDeque<>()).add(new Reply(status, json));
    }

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            calls.add(new Call(exchange.getRequestMethod(), path, exchange.getRequestURI().getQuery(),
                    exchange.getRequestHeaders().getFirst("Authorization"),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            Deque<Reply> queue = replies.getOrDefault(path, new ArrayDeque<>());
            Reply reply = queue.size() > 1 ? queue.poll() : queue.peek();
            if (reply == null) {
                reply = new Reply(404, "{}");
            }
            byte[] bytes = reply.json().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(reply.status(), bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        jira = new JiraClient("http://127.0.0.1:" + server.getAddress().getPort(), "bot@example.org", "tok",
                RetryPolicy.builder().maxRetries(2).delay(Duration.ofMillis(10)).build());
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void readsAnIssueWithItsRenderedDescriptionUsingBasicAuth() {
        reply("/rest/api/3/issue/ALE-42", 200,
                "{\"id\":\"1\",\"key\":\"ALE-42\",\"fields\":{\"summary\":\"Fix it\"},"
                        + "\"renderedFields\":{\"description\":\"<p>Broken <b>pump</b></p>\"}}");

        var issue = jira.issues().getIssue("ALE-42", List.of("summary", "description"), null, "renderedFields", null, null, null);

        assertThat(issue.getKey()).isEqualTo("ALE-42");
        assertThat(issue.getFields()).containsEntry("summary", "Fix it");
        assertThat(issue.getRenderedFields()).containsEntry("description", "<p>Broken <b>pump</b></p>");
        assertThat(calls.getFirst().authorization())
                .isEqualTo("Basic " + Base64.getEncoder().encodeToString("bot@example.org:tok".getBytes(StandardCharsets.UTF_8)));
        assertThat(calls.getFirst().query()).contains("expand=renderedFields");
    }

    @Test
    void searchesWithJql() {
        reply("/rest/api/3/search/jql", 200, "{\"issues\":[{\"id\":\"1\",\"key\":\"ALE-1\"}],\"isLast\":true}");

        var result = jira.search().searchAndReconsileIssuesUsingJqlPost(
                new SearchAndReconcileRequestBean().jql("labels = factory").maxResults(10));

        assertThat(result.getIssues()).extracting("key").containsExactly("ALE-1");
        assertThat(calls.getFirst().body()).contains("\"jql\":\"labels = factory\"");
    }

    @Test
    void commentsInAtlassianDocumentFormat() {
        reply("/rest/api/3/issue/ALE-42/comment", 201, "{\"id\":\"c1\"}");

        jira.comments().addComment("ALE-42", new Comment().body(JiraDocuments.text("PR opened.\n\nSee #12")), null);

        assertThat(calls.getFirst().body())
                .contains("\"type\":\"doc\"")
                .contains("\"text\":\"PR opened.\"")
                .contains("\"text\":\"See #12\"");
    }

    @Test
    void transitionsAnIssue() {
        reply("/rest/api/3/issue/ALE-42/transitions", 204, "");

        jira.issues().doTransition("ALE-42", new IssueUpdateDetails().transition(new IssueTransition().id("31")));

        assertThat(calls.getFirst().method()).isEqualTo("POST");
        assertThat(calls.getFirst().body()).contains("\"id\":\"31\"");
    }

    @Test
    void retriesRateLimitedRequests() {
        reply("/rest/api/3/myself", 429, "{}");
        reply("/rest/api/3/myself", 200, "{\"accountId\":\"a-1\"}");

        assertThat(jira.myself().getCurrentUser(null).getAccountId()).isEqualTo("a-1");
        assertThat(calls).hasSize(2);
    }

    @Test
    void givesUpAfterTheRetries() {
        reply("/rest/api/3/myself", 429, "{}");

        assertThatThrownBy(() -> jira.myself().getCurrentUser(null))
                .isInstanceOfSatisfying(HttpClientErrorException.class,
                        e -> assertThat(e.getStatusCode().value()).isEqualTo(429));
        assertThat(calls).hasSize(3);
    }

    @Test
    void clientErrorsAreNotRetried() {
        reply("/rest/api/3/issue/NOPE-1", 404, "{\"errorMessages\":[\"Issue does not exist\"]}");

        assertThatThrownBy(() -> jira.issues().getIssue("NOPE-1", null, null, null, null, null, null))
                .isInstanceOf(org.springframework.web.client.RestClientResponseException.class);
        assertThat(calls).hasSize(1);
    }
}
