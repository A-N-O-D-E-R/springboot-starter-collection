package com.anode.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.anode.logging.filter.EventMarkerFilter;
import com.anode.logging.filter.NonEventMarkerFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class EventsLoggerTest {

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(EventsLogger.class);
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void logsWithEventMarker() {
        EventsLogger.create("test").log("something happened");

        assertThat(appender.list).hasSize(1);
        ILoggingEvent event = appender.list.get(0);
        assertThat(new EventMarkerFilter().decide(event)).isEqualTo(ch.qos.logback.core.spi.FilterReply.ACCEPT);
        assertThat(new NonEventMarkerFilter().decide(event)).isEqualTo(ch.qos.logback.core.spi.FilterReply.DENY);
    }

    @Test
    void includesCategoryKeyValue() {
        EventsLogger.create("orders").log("order created");

        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getKeyValuePairs())
            .anyMatch(kv -> "category".equals(kv.key) && "orders".equals(kv.value));
    }

    @Test
    void includesAttribute() {
        EventsLogger.create("test").attribute("orderId", "ORD-123").log("msg");

        ILoggingEvent event = appender.list.get(0);
        assertThat(event.getKeyValuePairs())
            .anyMatch(kv -> "orderId".equals(kv.key) && "ORD-123".equals(kv.value));
    }

    @Test
    void clearsAttributesAfterLog() {
        EventsLogger eventsLogger = EventsLogger.create("test");
        eventsLogger.attribute("key", "value").log("first");
        eventsLogger.log("second");

        ILoggingEvent second = appender.list.get(1);
        assertThat(second.getKeyValuePairs()).noneMatch(kv -> "key".equals(kv.key));
    }

    @Test
    void persistsRequestAttributesAcrossLogs() {
        EventsLogger eventsLogger = EventsLogger.create("test").requestAttribute("requestId", "REQ-1");
        eventsLogger.log("first");
        eventsLogger.log("second");

        assertThat(appender.list.get(0).getKeyValuePairs())
            .anyMatch(kv -> "requestId".equals(kv.key) && "REQ-1".equals(kv.value));
        assertThat(appender.list.get(1).getKeyValuePairs())
            .anyMatch(kv -> "requestId".equals(kv.key) && "REQ-1".equals(kv.value));
    }

    @Test
    void removesRequestAttributes() {
        EventsLogger eventsLogger = EventsLogger.create("test").requestAttribute("requestId", "REQ-1");
        eventsLogger.log("first");
        eventsLogger.removeRequestAttributes().log("second");

        assertThat(appender.list.get(1).getKeyValuePairs()).noneMatch(kv -> "requestId".equals(kv.key));
    }

    @Test
    void logsWithoutMessageWhenNoneProvided() {
        EventsLogger.create("test").log();

        assertThat(appender.list).hasSize(1);
    }
}
