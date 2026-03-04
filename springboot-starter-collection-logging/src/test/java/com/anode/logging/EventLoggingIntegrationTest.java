package com.anode.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import com.anode.logging.encoder.EventJsonEncoder;
import com.anode.logging.filter.EventMarkerFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test verifying that the full pipeline (filter + encoder + appender) works correctly.
 */
class EventLoggingIntegrationTest {

    @TempDir
    Path tempDir;

    private FileAppender<ILoggingEvent> appender;
    private Logger logger;
    private Path eventsFile;

    record Order(String orderId, double amount) {}

    @BeforeEach
    void setUp() {
        eventsFile = tempDir.resolve("events.json");

        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        EventJsonEncoder encoder = new EventJsonEncoder();
        encoder.setContext(context);
        encoder.start();

        EventMarkerFilter filter = new EventMarkerFilter();
        filter.setContext(context);
        filter.start();

        appender = new FileAppender<>();
        appender.setContext(context);
        appender.setName("TEST_FILE");
        appender.setFile(eventsFile.toString());
        appender.setEncoder(encoder);
        appender.addFilter(filter);
        appender.start();

        logger = (Logger) LoggerFactory.getLogger("test.integration");
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    @Test
    void writesEventObjectToFile() throws IOException {
        logger.info(EventMarkers.EVENT, "{}", new Order("ORD-1", 99.99));

        List<String> lines = Files.readAllLines(eventsFile);
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0))
            .contains("\"type\":\"Order\"")
            .contains("\"orderId\":\"ORD-1\"");
    }

    @Test
    void doesNotWriteNonEventLogs() throws IOException {
        logger.info("regular log without event marker");

        if (Files.exists(eventsFile)) {
            assertThat(Files.readAllLines(eventsFile)).isEmpty();
        }
    }

    @Test
    void writesMultipleEvents() throws IOException {
        logger.info(EventMarkers.EVENT, "{}", new Order("ORD-1", 10.0));
        logger.info(EventMarkers.EVENT, "{}", new Order("ORD-2", 20.0));

        List<String> lines = Files.readAllLines(eventsFile);
        assertThat(lines).hasSize(2);
    }
}
