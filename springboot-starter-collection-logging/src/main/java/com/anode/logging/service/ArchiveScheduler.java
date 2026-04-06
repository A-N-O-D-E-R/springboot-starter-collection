package com.anode.logging.service;

import org.springframework.scheduling.annotation.Scheduled;

public class ArchiveScheduler {

    private final ArchiveService service;

    public ArchiveScheduler(ArchiveService service) {
        this.service = service;
    }

    @Scheduled(cron = "${logging.collection.archive.cron:0 0 2 * * *}")
    public void run() {
        service.archiveOldLogs();
    }
}