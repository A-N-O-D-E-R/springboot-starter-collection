package com.anode.logging.reader;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Reads events from JSON log files (JSON Lines format).
 */
public class EventLogReader {

    private final ObjectMapper objectMapper;
    private final Path logDirectory;
    private final String baseFileName;

    public EventLogReader(Path logDirectory, String baseFileName) {
        this.logDirectory = logDirectory;
        this.baseFileName = baseFileName;
        this.objectMapper = createObjectMapper();
    }

    public EventLogReader(Path logDirectory) {
        this(logDirectory, "events.json");
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    /**
     * Read all events of a specific type from log files.
     */
    public <T> List<T> readEvents(Class<T> eventType) {
        return readEvents(eventType, null, null, null);
    }

    /**
     * Read events with filtering by type name and date range.
     */
    public <T> List<T> readEvents(Class<T> eventType, String typeName,
                                   LocalDateTime startDate, LocalDateTime endDate) {
        List<T> events = new ArrayList<>();
        String targetType = typeName != null ? typeName : eventType.getSimpleName();

        try {
            List<Path> logFiles = getLogFiles(startDate, endDate);
            for (Path logFile : logFiles) {
                readEventsFromFile(logFile, eventType, targetType, startDate, endDate, events);
            }
        } catch (IOException e) {
            throw new EventLogReadException("Failed to read event logs", e);
        }

        return events;
    }

    /**
     * Read events with a custom filter predicate on the JSON node.
     */
    public <T> List<T> readEvents(Class<T> eventType, Predicate<JsonNode> filter) {
        List<T> events = new ArrayList<>();

        try {
            List<Path> logFiles = getLogFiles(null, null);
            for (Path logFile : logFiles) {
                readEventsFromFileWithFilter(logFile, eventType, filter, events);
            }
        } catch (IOException e) {
            throw new EventLogReadException("Failed to read event logs", e);
        }

        return events;
    }

    private List<Path> getLogFiles(LocalDateTime startDate, LocalDateTime endDate) throws IOException {
        List<Path> files = new ArrayList<>();
        Path currentFile = logDirectory.resolve(baseFileName);

        if (Files.exists(currentFile)) {
            files.add(currentFile);
        }

        // Find rotated files matching pattern: events.json.yyyy-MM-dd
        try (Stream<Path> paths = Files.list(logDirectory)) {
            paths.filter(p -> p.getFileName().toString().startsWith(baseFileName + "."))
                 .filter(p -> isFileInDateRange(p, startDate, endDate))
                 .sorted()
                 .forEach(files::add);
        }

        return files;
    }

    private boolean isFileInDateRange(Path file, LocalDateTime startDate, LocalDateTime endDate) {
        String fileName = file.getFileName().toString();
        String datePart = fileName.substring(baseFileName.length() + 1);

        try {
            LocalDate fileDate = LocalDate.parse(datePart);
            LocalDate start = startDate != null ? startDate.toLocalDate() : LocalDate.MIN;
            LocalDate end = endDate != null ? endDate.toLocalDate() : LocalDate.MAX;
            return !fileDate.isBefore(start) && !fileDate.isAfter(end);
        } catch (Exception e) {
            return false;
        }
    }

    private <T> void readEventsFromFile(Path file, Class<T> eventType, String targetType,
                                         LocalDateTime startDate, LocalDateTime endDate,
                                         List<T> events) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                try {
                    JsonNode node = objectMapper.readTree(line);
                    String type = node.path("type").asText();

                    if (!targetType.equals(type)) continue;

                    if (startDate != null || endDate != null) {
                        JsonNode timestampNode = node.path("timestamp");
                        if (!timestampNode.isMissingNode()) {
                            LocalDateTime timestamp = objectMapper.treeToValue(
                                timestampNode, LocalDateTime.class
                            );
                            if (startDate != null && timestamp.isBefore(startDate)) continue;
                            if (endDate != null && timestamp.isAfter(endDate)) continue;
                        }
                    }

                    T event = objectMapper.treeToValue(node, eventType);
                    events.add(event);
                } catch (Exception e) {
                    // Skip malformed lines
                }
            }
        }
    }

    private <T> void readEventsFromFileWithFilter(Path file, Class<T> eventType,
                                                   Predicate<JsonNode> filter,
                                                   List<T> events) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                try {
                    JsonNode node = objectMapper.readTree(line);
                    if (filter.test(node)) {
                        T event = objectMapper.treeToValue(node, eventType);
                        events.add(event);
                    }
                } catch (Exception e) {
                    // Skip malformed lines
                }
            }
        }
    }

    /**
     * Get available dates from rotated log files.
     */
    public List<LocalDate> getAvailableDates() {
        List<LocalDate> dates = new ArrayList<>();

        try (Stream<Path> paths = Files.list(logDirectory)) {
            paths.filter(p -> p.getFileName().toString().startsWith(baseFileName + "."))
                 .forEach(p -> {
                     String fileName = p.getFileName().toString();
                     String datePart = fileName.substring(baseFileName.length() + 1);
                     try {
                         dates.add(LocalDate.parse(datePart));
                     } catch (Exception ignored) {}
                 });
        } catch (IOException e) {
            // Return empty list on error
        }

        // Add today if current file exists
        if (Files.exists(logDirectory.resolve(baseFileName))) {
            dates.add(LocalDate.now());
        }

        dates.sort(LocalDate::compareTo);
        return dates;
    }

    public static class EventLogReadException extends RuntimeException {
        public EventLogReadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
