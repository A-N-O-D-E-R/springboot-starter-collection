package com.anode.logging.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Reads events from XML log files (one XML element per line).
 */
public class EventXmlLogReader {

    private static final Pattern ELEMENT_PATTERN = Pattern.compile("<(\\w+)\\s*([^>]*)/?>");
    private static final Pattern ATTR_PATTERN = Pattern.compile("(\\w+)=\"([^\"]*)\"");

    private final Path logDirectory;
    private final String baseFileName;

    public EventXmlLogReader(Path logDirectory, String baseFileName) {
        this.logDirectory = logDirectory;
        this.baseFileName = baseFileName;
    }

    public EventXmlLogReader(Path logDirectory) {
        this(logDirectory, "events.xml");
    }

    /**
     * Read events by type name, mapping attributes to an object using the provided factory.
     */
    public <T> List<T> readEvents(String typeName, Function<Map<String, String>, T> factory) {
        return readEvents(typeName, factory, null, null);
    }

    /**
     * Read events with date range filtering.
     */
    public <T> List<T> readEvents(String typeName, Function<Map<String, String>, T> factory,
                                   LocalDateTime startDate, LocalDateTime endDate) {
        List<T> events = new ArrayList<>();

        try {
            List<Path> logFiles = getLogFiles(startDate, endDate);
            for (Path logFile : logFiles) {
                readEventsFromFile(logFile, typeName, factory, startDate, endDate, events);
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

    private <T> void readEventsFromFile(Path file, String typeName,
                                         Function<Map<String, String>, T> factory,
                                         LocalDateTime startDate, LocalDateTime endDate,
                                         List<T> events) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                try {
                    Matcher elementMatcher = ELEMENT_PATTERN.matcher(line);
                    if (!elementMatcher.find()) continue;

                    String elementName = elementMatcher.group(1);
                    if (!typeName.equals(elementName)) continue;

                    String attributes = elementMatcher.group(2);
                    Map<String, String> attrMap = parseAttributes(attributes);

                    if (startDate != null || endDate != null) {
                        String timestampStr = attrMap.get("timestamp");
                        if (timestampStr != null) {
                            LocalDateTime timestamp = LocalDateTime.parse(timestampStr);
                            if (startDate != null && timestamp.isBefore(startDate)) continue;
                            if (endDate != null && timestamp.isAfter(endDate)) continue;
                        }
                    }

                    T event = factory.apply(attrMap);
                    if (event != null) {
                        events.add(event);
                    }
                } catch (Exception e) {
                    // Skip malformed lines
                }
            }
        }
    }

    private Map<String, String> parseAttributes(String attributes) {
        Map<String, String> map = new HashMap<>();
        Matcher attrMatcher = ATTR_PATTERN.matcher(attributes);
        while (attrMatcher.find()) {
            String key = attrMatcher.group(1);
            String value = unescapeXml(attrMatcher.group(2));
            map.put(key, value);
        }
        return map;
    }

    private String unescapeXml(String value) {
        return value
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'");
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
