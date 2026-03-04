package com.anode.logging.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventJsonEncoderTest {

    private final EventJsonEncoder encoder = new EventJsonEncoder();
    private final ObjectMapper objectMapper = new ObjectMapper();

    record Order(String orderId, BigDecimal amount) {}

    @Test
    void encodesPojo() throws IOException {
        ILoggingEvent event = eventWithArgs(new Order("ORD-1", new BigDecimal("99.99")));

        JsonNode node = parse(encoder.encode(event));

        assertThat(node.get("type").asText()).isEqualTo("Order");
        assertThat(node.get("orderId").asText()).isEqualTo("ORD-1");
        assertThat(node.get("amount").asText()).isEqualTo("99.99");
    }

    @Test
    void encodesMap() throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("action", "login");
        map.put("userId", 42);
        ILoggingEvent event = eventWithArgs(map);

        JsonNode node = parse(encoder.encode(event));

        assertThat(node.get("action").asText()).isEqualTo("login");
        assertThat(node.get("userId").asInt()).isEqualTo(42);
    }

    @Test
    void encodesNullArgsAsLogEvent() throws IOException {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getArgumentArray()).thenReturn(null);
        when(event.getFormattedMessage()).thenReturn("something happened");

        JsonNode node = parse(encoder.encode(event));

        assertThat(node.get("type").asText()).isEqualTo("LogEvent");
        assertThat(node.get("message").asText()).isEqualTo("something happened");
    }

    @Test
    void encodesEmptyArgsAsLogEvent() throws IOException {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getArgumentArray()).thenReturn(new Object[0]);
        when(event.getFormattedMessage()).thenReturn("msg");

        JsonNode node = parse(encoder.encode(event));

        assertThat(node.get("type").asText()).isEqualTo("LogEvent");
    }

    @Test
    void appendsNewline() {
        ILoggingEvent event = eventWithArgs(new Order("ORD-1", BigDecimal.ONE));
        byte[] result = encoder.encode(event);
        assertThat((char) result[result.length - 1]).isEqualTo('\n');
    }

    private ILoggingEvent eventWithArgs(Object... args) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getArgumentArray()).thenReturn(args);
        return event;
    }

    private JsonNode parse(byte[] bytes) throws IOException {
        return objectMapper.readTree(new String(bytes, StandardCharsets.UTF_8).strip());
    }
}
