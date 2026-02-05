package com.example.logging;

import com.anode.logging.EventMarkers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@SpringBootApplication
public class LoggingDemoApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LoggingDemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(LoggingDemoApplication.class, args);
    }

    @Override
    public void run(String... args) {
        // Regular log - goes to console only, NOT to events files
        log.info("Application started");

        // Example 1: Simple order event
        log.info(EventMarkers.EVENT, "{}", new Order(
            "ORD-123",
            new BigDecimal("99.99"),
            "USD"
        ));

        // Example 2: User login event
        log.info(EventMarkers.EVENT, "{}", new UserLogin(
            "user456",
            "192.168.1.100",
            Instant.parse("2024-01-15T10:30:00Z"),
            true
        ));

        // Example 3: Payment event with nested object
        log.info(EventMarkers.EVENT, "{}", new Payment(
            "PAY-789",
            new BigDecimal("150.00"),
            "completed",
            new Customer("C-001", "john@example.com")
        ));

        // Example 4: Inventory event with list
        log.info(EventMarkers.EVENT, "{}", new InventoryUpdate(
            "SKU-001",
            50,
            List.of("warehouse-A", "warehouse-B")
        ));

        log.info("Events logged successfully");
    }

    // Event records
    public record Order(String orderId, BigDecimal amount, String currency) {}

    public record UserLogin(String userId, String ipAddress, Instant timestamp, boolean success) {}

    public record Payment(String paymentId, BigDecimal amount, String status, Customer customer) {}

    public record Customer(String id, String email) {}

    public record InventoryUpdate(String sku, int quantity, List<String> locations) {}
}

/*
 * OUTPUT EXAMPLES:
 *
 * === events.json ===
 * {"type":"Order","orderId":"ORD-123","amount":99.99,"currency":"USD"}
 * {"type":"UserLogin","userId":"user456","ipAddress":"192.168.1.100","timestamp":"2024-01-15T10:30:00Z","success":true}
 * {"type":"Payment","paymentId":"PAY-789","amount":150.00,"status":"completed","customer":{"id":"C-001","email":"john@example.com"}}
 * {"type":"InventoryUpdate","sku":"SKU-001","quantity":50,"locations":["warehouse-A","warehouse-B"]}
 *
 * === events.xml ===
 * <Order orderId="ORD-123" amount="99.99" currency="USD"/>
 * <UserLogin userId="user456" ipAddress="192.168.1.100" timestamp="2024-01-15T10:30:00Z" success="true"/>
 * <Payment paymentId="PAY-789" amount="150.00" status="completed"><customer id="C-001" email="john@example.com"/></Payment>
 * <InventoryUpdate sku="SKU-001" quantity="50"><locations value="warehouse-A"/><locations value="warehouse-B"/></InventoryUpdate>
 */
