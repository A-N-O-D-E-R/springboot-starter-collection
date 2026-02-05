package com.anode.logging.encoder;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.EncoderBase;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

/**
 * Logback encoder that serializes the first argument of EVENT-marked log entries to XML format.
 * Output format: &lt;ClassName field1="value1" field2="value2"/&gt;
 */
public class EventXmlEncoder extends EncoderBase<ILoggingEvent> {

    private static final byte[] NEWLINE = System.lineSeparator().getBytes(StandardCharsets.UTF_8);

    private final ObjectMapper objectMapper;

    public EventXmlEncoder() {
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
                return encodeXml("LogEvent", Map.of("message", event.getFormattedMessage()));
            }
            return encodeXml(payload.getClass().getSimpleName(), payload);
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

    private byte[] encodeXml(String elementName, Object payload) {
        StringBuilder sb = new StringBuilder();
        sb.append('<').append(escapeXmlName(elementName));

        JsonNode node = objectMapper.valueToTree(payload);

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            StringBuilder childElements = new StringBuilder();

            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldName = field.getKey();
                JsonNode value = field.getValue();

                if (value.isObject() || value.isArray()) {
                    childElements.append(serializeComplexField(fieldName, value));
                } else {
                    sb.append(' ')
                      .append(escapeXmlName(fieldName))
                      .append("=\"")
                      .append(escapeXmlValue(nodeToString(value)))
                      .append('"');
                }
            }

            if (childElements.length() > 0) {
                sb.append('>');
                sb.append(childElements);
                sb.append("</").append(escapeXmlName(elementName)).append('>');
            } else {
                sb.append("/>");
            }
        } else {
            sb.append(" value=\"").append(escapeXmlValue(nodeToString(node))).append("\"/>");
        }

        return appendNewline(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String serializeComplexField(String fieldName, JsonNode node) {
        StringBuilder sb = new StringBuilder();

        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode element : arrayNode) {
                sb.append('<').append(escapeXmlName(fieldName));
                if (element.isObject()) {
                    appendObjectAttributes(sb, (ObjectNode) element);
                } else {
                    sb.append(" value=\"").append(escapeXmlValue(nodeToString(element))).append('"');
                }
                sb.append("/>");
            }
        } else if (node.isObject()) {
            sb.append('<').append(escapeXmlName(fieldName));
            appendObjectAttributes(sb, (ObjectNode) node);
            sb.append("/>");
        }

        return sb.toString();
    }

    private void appendObjectAttributes(StringBuilder sb, ObjectNode objectNode) {
        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            JsonNode value = field.getValue();
            if (!value.isObject() && !value.isArray()) {
                sb.append(' ')
                  .append(escapeXmlName(field.getKey()))
                  .append("=\"")
                  .append(escapeXmlValue(nodeToString(value)))
                  .append('"');
            }
        }
    }

    private String nodeToString(JsonNode node) {
        if (node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isNumber()) {
            return node.asText();
        }
        if (node.isBoolean()) {
            return String.valueOf(node.asBoolean());
        }
        return node.toString();
    }

    private String escapeXmlName(String name) {
        if (name == null || name.isEmpty()) {
            return "element";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (i == 0) {
                if (Character.isLetter(c) || c == '_') {
                    sb.append(c);
                } else {
                    sb.append('_');
                }
            } else {
                if (Character.isLetterOrDigit(c) || c == '_' || c == '-' || c == '.') {
                    sb.append(c);
                } else {
                    sb.append('_');
                }
            }
        }
        return sb.toString();
    }

    private String escapeXmlValue(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&apos;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private byte[] encodeError(Exception error) {
        String msg = error.getMessage() != null ? escapeXmlValue(error.getMessage()) : "unknown";
        String xml = "<SerializationError message=\"" + msg + "\"/>" + System.lineSeparator();
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] appendNewline(byte[] data) {
        byte[] result = new byte[data.length + NEWLINE.length];
        System.arraycopy(data, 0, result, 0, data.length);
        System.arraycopy(NEWLINE, 0, result, data.length, NEWLINE.length);
        return result;
    }

    @Override
    public byte[] footerBytes() {
        return null;
    }
}
