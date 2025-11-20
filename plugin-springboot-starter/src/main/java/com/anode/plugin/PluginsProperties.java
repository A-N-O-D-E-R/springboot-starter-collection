package com.anode.plugin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "plugins")
public class PluginsProperties {

    /**
     * Enable PF4J plugin loading.
     */
    private boolean enabled = true;

    /**
     * Root folder for plugins (relative or absolute path).
     */
    private String pluginsRootFolder = "plugins";

    public boolean isEnabled() {
        return enabled;
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPluginsRootFolder() {
        return pluginsRootFolder;
    }
    public void setPluginsRootFolder(String pluginsRootFolder) {
        this.pluginsRootFolder = pluginsRootFolder;
    }
}
