package com.anode.logging.reader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventLogReaderTest {

    @TempDir
    Path logDir;

    record Order(String type, String orderId, double amount) {}

    @Test
    void readsEventsFromCurrentFile() throws IOException {
        writeLines("events.json",
            "{\"type\":\"Order\",\"orderId\":\"ORD-1\",\"amount\":10.0}",
            "{\"type\":\"Order\",\"orderId\":\"ORD-2\",\"amount\":20.0}"
        );

        List<Order> events = new EventLogReader(logDir).readEvents(Order.class);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).orderId()).isEqualTo("ORD-1");
        assertThat(events.get(1).orderId()).isEqualTo("ORD-2");
    }

    @Test
    void filtersEventsByTypeName() throws IOException {
        writeLines("events.json",
            "{\"type\":\"Order\",\"orderId\":\"ORD-1\",\"amount\":10.0}",
            "{\"type\":\"Payment\",\"orderId\":\"PAY-1\",\"amount\":5.0}"
        );

        List<Order> events = new EventLogReader(logDir).readEvents(Order.class, "Order", null, null);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).orderId()).isEqualTo("ORD-1");
    }

    @Test
    void filtersEventsByDateRange() throws IOException {
        writeLines("events.json",
            "{\"type\":\"Order\",\"orderId\":\"ORD-1\",\"amount\":1.0,\"timestamp\":\"2024-01-10T10:00:00\"}",
            "{\"type\":\"Order\",\"orderId\":\"ORD-2\",\"amount\":2.0,\"timestamp\":\"2024-01-20T10:00:00\"}"
        );

        List<Order> events = new EventLogReader(logDir).readEvents(
            Order.class, "Order",
            LocalDateTime.of(2024, 1, 15, 0, 0),
            LocalDateTime.of(2024, 1, 31, 0, 0)
        );

        assertThat(events).hasSize(1);
        assertThat(events.get(0).orderId()).isEqualTo("ORD-2");
    }

    @Test
    void readsEventsWithCustomPredicate() throws IOException {
        writeLines("events.json",
            "{\"type\":\"Order\",\"orderId\":\"ORD-1\",\"amount\":10.0}",
            "{\"type\":\"Order\",\"orderId\":\"ORD-2\",\"amount\":50.0}"
        );

        List<Order> events = new EventLogReader(logDir).readEvents(
            Order.class,
            node -> node.path("amount").asDouble() > 20.0
        );

        assertThat(events).hasSize(1);
        assertThat(events.get(0).orderId()).isEqualTo("ORD-2");
    }

    @Test
    void skipsBlankAndMalformedLines() throws IOException {
        writeLines("events.json",
            "{\"type\":\"Order\",\"orderId\":\"ORD-1\",\"amount\":1.0}",
            "",
            "not-valid-json",
            "{\"type\":\"Order\",\"orderId\":\"ORD-2\",\"amount\":2.0}"
        );

        List<Order> events = new EventLogReader(logDir).readEvents(Order.class);

        assertThat(events).hasSize(2);
    }

    @Test
    void readsFromRotatedFiles() throws IOException {
        writeLines("events.json",
            "{\"type\":\"Order\",\"orderId\":\"ORD-TODAY\",\"amount\":1.0}"
        );
        writeLines("events.json." + LocalDate.now().minusDays(1),
            "{\"type\":\"Order\",\"orderId\":\"ORD-YESTERDAY\",\"amount\":2.0}"
        );

        List<Order> events = new EventLogReader(logDir).readEvents(Order.class);

        assertThat(events).extracting(Order::orderId).containsExactlyInAnyOrder("ORD-TODAY", "ORD-YESTERDAY");
    }

    @Test
    void returnsEmptyListWhenNoFiles() {
        List<Order> events = new EventLogReader(logDir).readEvents(Order.class);
        assertThat(events).isEmpty();
    }

    @Test
    void getAvailableDatesIncludesRotatedFiles() throws IOException {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        writeLines("events.json." + yesterday, "{\"type\":\"Order\",\"orderId\":\"ORD-1\",\"amount\":1.0}");
        writeLines("events.json", "{\"type\":\"Order\",\"orderId\":\"ORD-2\",\"amount\":2.0}");

        List<LocalDate> dates = new EventLogReader(logDir).getAvailableDates();

        assertThat(dates).contains(yesterday, LocalDate.now());
    }

    private void writeLines(String fileName, String... lines) throws IOException {
        Files.writeString(logDir.resolve(fileName), String.join("\n", lines) + "\n");
    }

}
