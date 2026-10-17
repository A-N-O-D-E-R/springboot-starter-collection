package com.anode.github;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHPermissionType;

/** GitHubApp against a local stand-in for the GitHub REST API. */
class GitHubAppTest {

    record Call(String method, String path, String authorization, String body) {}

    private HttpServer server;
    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private String apiUrl;
    private String privateKeyPem;

    private void respond(String path, String json) {
        server.createContext(path, exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            calls.add(new Call(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Authorization"), body));
            byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
    }

    @BeforeEach
    void start() throws Exception {
        var keys = KeyPairGenerator.getInstance("RSA");
        keys.initialize(2048);
        privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder().encodeToString(keys.generateKeyPair().getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        apiUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        String expiry = Instant.now().plusSeconds(3600).toString();
        respond("/app/installations/77/access_tokens",
                "{\"token\":\"ghs_scoped\",\"expires_at\":\"" + expiry + "\",\"permissions\":{\"contents\":\"write\"}}");
        respond("/app", "{\"id\":1,\"slug\":\"factory\",\"name\":\"factory\"}");
        respond("/repos/acme/api/installation", "{\"id\":77,\"app_id\":1}");
        respond("/repos/acme/api", "{\"id\":5,\"name\":\"api\",\"full_name\":\"acme/api\"}");
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void createsATokenScopedToOneRepositoryAndThePermissionsAsked() {
        var app = new GitHubApp(apiUrl, "1", privateKeyPem);

        RepositoryToken token = app.repositoryToken("acme", "api", Map.of("contents", GHPermissionType.WRITE));

        assertThat(token.token()).isEqualTo("ghs_scoped");
        assertThat(token.expiresAt()).isAfter(Instant.now());
        assertThat(token.toString()).doesNotContain("ghs_scoped");
        Call create = calls.stream().filter(c -> c.path().endsWith("/access_tokens")).findFirst().orElseThrow();
        assertThat(create.authorization()).startsWith("Bearer "); // signed as the App
        assertThat(create.body()).contains("\"repositories\":[\"api\"]").contains("\"contents\":\"write\"");
    }

    @Test
    void repositoryClientsAuthenticateAsTheInstallation() throws IOException {
        var app = new GitHubApp(apiUrl, "1", privateKeyPem);

        var repository = app.forRepository("acme", "api").getRepository("acme/api");

        assertThat(repository.getFullName()).isEqualTo("acme/api");
        Call get = calls.stream().filter(c -> c.path().equals("/repos/acme/api")).findFirst().orElseThrow();
        assertThat(get.authorization()).isEqualTo("token ghs_scoped");
        assertThat(app.forRepository("acme", "api")).isSameAs(app.forRepository("acme", "api"));
    }
}
