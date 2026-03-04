package com.anode.logging.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EventXmlEncoderTest {

    private final EventXmlEncoder encoder = new EventXmlEncoder();

    record Address(String street, String city) {}
    record Person(String name, Address address) {}
    record Order(String orderId, List<String> tags) {}

    @Test
    void encodesSimplePojo() {
        ILoggingEvent event = eventWithArgs(new Address("Main St", "Springfield"));
        String xml = encode(event);
        assertThat(xml).startsWith("<Address");
        assertThat(xml).contains("street=\"Main St\"");
        assertThat(xml).contains("city=\"Springfield\"");
    }

    @Test
    void encodesNestedObject() {
        ILoggingEvent event = eventWithArgs(new Person("Alice", new Address("Oak Ave", "Shelbyville")));
        String xml = encode(event);
        assertThat(xml).startsWith("<Person name=\"Alice\">");
        assertThat(xml).contains("<address street=\"Oak Ave\" city=\"Shelbyville\"/>");
        assertThat(xml).endsWith("</Person>\n");
    }

    @Test
    void encodesArrayField() {
        ILoggingEvent event = eventWithArgs(new Order("ORD-1", List.of("urgent", "fragile")));
        String xml = encode(event);
        assertThat(xml).contains("<tags value=\"urgent\"/>");
        assertThat(xml).contains("<tags value=\"fragile\"/>");
    }

    @Test
    void encodesNullArgsAsLogEvent() {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getArgumentArray()).thenReturn(null);
        when(event.getFormattedMessage()).thenReturn("hello");
        String xml = encode(event);
        assertThat(xml).contains("<LogEvent");
        assertThat(xml).contains("message=\"hello\"");
    }

    @Test
    void escapesSpecialCharacters() {
        record Payload(String value) {}
        ILoggingEvent event = eventWithArgs(new Payload("a&b<c>d\"e'f"));
        String xml = encode(event);
        assertThat(xml).contains("value=\"a&amp;b&lt;c&gt;d&quot;e&apos;f\"");
    }

    @Test
    void appendsNewline() {
        ILoggingEvent event = eventWithArgs(new Address("x", "y"));
        byte[] result = encoder.encode(event);
        assertThat((char) result[result.length - 1]).isEqualTo('\n');
    }

    private ILoggingEvent eventWithArgs(Object... args) {
        ILoggingEvent event = mock(ILoggingEvent.class);
        when(event.getArgumentArray()).thenReturn(args);
        return event;
    }

    private String encode(ILoggingEvent event) {
        return new String(encoder.encode(event), StandardCharsets.UTF_8);
    }
}
