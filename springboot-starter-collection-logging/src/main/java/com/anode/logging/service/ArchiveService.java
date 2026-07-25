package com.anode.logging.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;


public class ArchiveService {
    private static final Logger log = LoggerFactory.getLogger(ArchiveService.class);
    private static final String[] LOG_PREFIXES = {"events.json.", "events.xml."};
    
    private Path logDirectory;
    private int archiveAfterDays;

    public ArchiveService(Path logDirectory, int archiveAfterDays) {
        this.logDirectory = logDirectory;
        this.archiveAfterDays = archiveAfterDays;
    }

    @Async
    public void archiveOldLogs() {
        LocalDate threshold = LocalDate.now().minusDays(archiveAfterDays);
        try (Stream<Path> files = Files.list(logDirectory)) {
            files.filter(Files::isRegularFile)
                 .filter(this::isRotatedLogFile)
                 .filter(path -> !path.toString().endsWith(".gz"))
                 .filter(path -> isOlderThan(path, threshold))
                 .forEach(this::compressFile);
        } catch (IOException e) {
            log.error("Failed to list log directory", e);
        }

        log.info("Archive completed");
    }


     private boolean isRotatedLogFile(Path path) {
        String fileName = path.getFileName().toString();
        for (String prefix : LOG_PREFIXES) {
            if (fileName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private boolean isOlderThan(Path path, LocalDate threshold) {
        String fileName = path.getFileName().toString();

        for (String prefix : LOG_PREFIXES) {
            if (fileName.startsWith(prefix)) {
                String datePart = fileName.substring(prefix.length()).replace(".gz", "");
                try {
                    LocalDate fileDate = LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE);
                    return fileDate.isBefore(threshold);
                } catch (DateTimeParseException e) {
                    return false;
                }
            }
        }
        return false;
    }

    private void compressFile(Path source) {
        Path target = Path.of(source.toString() + ".gz");

        if (Files.exists(target)) {
            log.debug("Archive already exists: {}", target);
            return;
        }

        log.info("Compressing: {}", source.getFileName());

        try (InputStream in = Files.newInputStream(source);
             OutputStream out = Files.newOutputStream(target);
             GZIPOutputStream gzip = new GZIPOutputStream(out)) {

            in.transferTo(gzip);
            gzip.finish();

            Files.delete(source);
            log.info("Archived: {} -> {}", source.getFileName(), target.getFileName());

        } catch (IOException e) {
            log.error("Failed to compress: {}", source, e);
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignored) {}
        }
    }

}
