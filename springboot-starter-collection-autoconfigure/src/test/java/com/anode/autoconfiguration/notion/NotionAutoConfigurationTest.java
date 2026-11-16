package com.anode.autoconfiguration.notion;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import notion.api.v1.NotionClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class NotionAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NotionAutoConfiguration.class));

    @Test
    void noClientWithoutToken() {
        runner.run(context -> assertThat(context).doesNotHaveBean(NotionClient.class));
    }

    @Test
    void theClientCallsTheConfiguredApiWithTheToken() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/users/me", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] json = "{\"object\":\"user\",\"id\":\"u-1\",\"type\":\"bot\",\"name\":\"factory\",\"bot\":{}}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, json.length);
            exchange.getResponseBody().write(json);
            exchange.close();
        });
        server.start();
        try {
            runner.withPropertyValues("anode.notion.token=secret_test",
                            "anode.notion.base-url=http://127.0.0.1:" + server.getAddress().getPort() + "/v1")
                    .run(context -> assertThat(context.getBean(NotionClient.class).retrieveBotUser().getId()).isEqualTo("u-1"));
            assertThat(authorization.get()).isEqualTo("Bearer secret_test");
        } finally {
            server.stop(0);
        }
    }
}
