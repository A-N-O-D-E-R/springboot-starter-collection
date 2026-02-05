package com.anode.logging.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;

/**
 * Logback encoder that serializes the first argument of EVENT-marked log entries to JSON Lines format.
 * Output format: {"type":"ClassName",...fields...}
 */
public class EventJsonEncoder extends EncoderBase<ILoggingEvent> {

    private static final byte[] NEWLINE = System.lineSeparator().getBytes(StandardCharsets.UTF_8);

    private final ObjectMapper objectMapper;

    public EventJsonEncoder() {
        this.objectMapper = createObjectMapper();
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    @Override
    public byte[] headerBytes() {
        return null;
    }

    @Override
    public byte[] encode(ILoggingEvent event) {
        try {
            Object payload = extractPayload(event);
            if (payload == null) {
                return encodeWithType("LogEvent", Map.of("message", event.getFormattedMessage()));
            }
            return encodeWithType(payload.getClass().getSimpleName(), payload);
        } catch (Exception e) {
            return encodeError(e);
        }
    }

    private Object extractPayload(ILoggingEvent event) {
        Object[] args = event.getArgumentArray();
        if (args == null || args.length == 0) {
            return null;
        }
        return args[0];
    }

    private byte[] encodeWithType(String type, Object payload) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JsonGenerator gen = objectMapper.getFactory().createGenerator(baos);

        gen.writeStartObject();
        gen.writeStringField("type", type);

        // Write all fields from the payload object
        if (payload instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                gen.writeFieldName(String.valueOf(entry.getKey()));
                objectMapper.writeValue(gen, entry.getValue());
            }
        } else {
            // Serialize object to tree and merge fields
            var node = objectMapper.valueToTree(payload);
            var fields = node.fields();
            while (fields.hasNext()) {
                var field = fields.next();
                gen.writeFieldName(field.getKey());
                objectMapper.writeValue(gen, field.getValue());
            }
        }

        gen.writeEndObject();
        gen.close();

        return appendNewline(baos.toByteArray());
    }

    private byte[] encodeError(Exception error) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(Map.of(
                "type", "SerializationError",
                "error", error.getMessage() != null ? error.getMessage() : "unknown"
            ));
            return appendNewline(json);
        } catch (Exception e) {
            return ("{\"type\":\"SerializationError\"}" + System.lineSeparator())
                .getBytes(StandardCharsets.UTF_8);
        }
    }

    private byte[] appendNewline(byte[] json) {
        byte[] result = new byte[json.length + NEWLINE.length];
        System.arraycopy(json, 0, result, 0, json.length);
        System.arraycopy(NEWLINE, 0, result, json.length, NEWLINE.length);
        return result;
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }
}
