package com.anode.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Structured event logger for business events and metrics.
 * Provides a fluent API for logging events with attributes and categories.
 * Thread-safe using ThreadLocal storage for attributes.
 */
public class EventsLogger {

    private static final Logger log = LoggerFactory.getLogger(EventsLogger.class);

    private final String category;
    private final ThreadLocal<Map<String, Object>> attributes = ThreadLocal.withInitial(HashMap::new);
    private final ThreadLocal<Map<String, Object>> requestAttributes = ThreadLocal.withInitial(HashMap::new);

    private EventsLogger(String category) {
        this.category = category;
    }

    /**
     * Create a new EventsLogger for the given category.
     *
     * @param category the event category (e.g., "user-service", "payment-processor")
     * @return a new EventsLogger instance
     */
    public static EventsLogger create(String category) {
        return new EventsLogger(category);
    }

    /**
     * Set the action attribute for this event.
     *
     * @param action the action being performed (e.g., "user-login", "order-created")
     * @return this EventsLogger for method chaining
     */
    public EventsLogger action(String action) {
        return attribute("action", action);
    }

    /**
     * Add an attribute to this event.
     * Attributes are logged once and then cleared.
     *
     * @param key   the attribute key
     * @param value the attribute value
     * @return this EventsLogger for method chaining
     */
    public EventsLogger attribute(String key, Object value) {
        this.attributes.get().put(key, value);
        return this;
    }

    /**
     * Add a request-scoped attribute that persists across multiple log calls.
     * Must be explicitly cleared with removeRequestAttributes().
     *
     * @param key   the attribute key
     * @param value the attribute value
     * @return this EventsLogger for method chaining
     */
    public EventsLogger requestAttribute(String key, Object value) {
        this.requestAttributes.get().put(key, value);
        return this;
    }

    /**
     * Remove all request-scoped attributes.
     *
     * @return this EventsLogger for method chaining
     */
    public EventsLogger removeRequestAttributes() {
        requestAttributes.remove();
        return this;
    }

    /**
     * Log the event with a message and arguments.
     *
     * @param format    the message format (SLF4J style)
     * @param arguments the message arguments
     */
    public void log(String format, Object... arguments) {
        var loggingEventBuilder = log.atInfo().addKeyValue("category", category);

        attributes.get().forEach(loggingEventBuilder::addKeyValue);
        requestAttributes.get().forEach(loggingEventBuilder::addKeyValue);

        if (null == format) {
            loggingEventBuilder.log();
        } else {
            loggingEventBuilder.log(format, arguments);
        }

        attributes.remove();
    }

    /**
     * Log the event without a message.
     */
    public void log() {
        log(null);
    }

}
