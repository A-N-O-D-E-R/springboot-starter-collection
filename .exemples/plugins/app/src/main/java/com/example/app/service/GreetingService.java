package com.example.app.service;

import com.example.app.extension.GreetingExtension;
import org.pf4j.PluginManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GreetingService {

    private final PluginManager pluginManager;

    public GreetingService(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    /**
     * Get all available greetings from loaded plugins.
     *
     * @param name the name to greet
     * @return list of greeting messages from all plugins
     */
    public List<String> getAllGreetings(String name) {
        List<GreetingExtension> extensions = pluginManager.getExtensions(GreetingExtension.class);

        if (extensions.isEmpty()) {
            return List.of("No greeting plugins loaded. Default: Hello, " + name + "!");
        }

        return extensions.stream()
                .map(ext -> ext.getGreetingType() + ": " + ext.greet(name))
                .toList();
    }

    /**
     * Get the number of loaded greeting plugins.
     *
     * @return count of greeting extensions
     */
    public int getGreetingPluginCount() {
        return pluginManager.getExtensions(GreetingExtension.class).size();
    }

    /**
     * Get information about all loaded plugins.
     *
     * @return list of plugin information strings
     */
    public List<String> getLoadedPluginsInfo() {
        return pluginManager.getPlugins().stream()
                .map(plugin -> String.format("Plugin: %s (v%s) - %s",
                        plugin.getPluginId(),
                        plugin.getDescriptor().getVersion(),
                        plugin.getPluginState()))
                .toList();
    }
}
