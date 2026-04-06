package com.anode.autoconfiguration.logging.properties;

import java.nio.file.Path;

public class ArchiveProperties {

    private boolean enabled = true;
    private Path logDirectory;
    private int archiveAfterDays = 7;
    private boolean scheduled = true;
    
    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    public Path getLogDirectory() {
        return logDirectory;
    }
    public void setLogDirectory(Path logDirectory) {
        this.logDirectory = logDirectory;
    }
    public int getArchiveAfterDays() {
        return archiveAfterDays;
    }
    public void setArchiveAfterDays(int archiveAfterDays) {
        this.archiveAfterDays = archiveAfterDays;
    }
    public boolean isScheduled() {
        return scheduled;
    }
    public void setScheduled(boolean scheduled) {
        this.scheduled = scheduled;
    }

}