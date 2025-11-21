package com.anode.autoconfiguration.plugin;

import com.anode.plugin.PluginsProperties;
import org.pf4j.DefaultPluginManager;
import org.pf4j.PluginManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Paths;

@Configuration
@EnableConfigurationProperties(PluginsProperties.class)
@ConditionalOnProperty(name = "plugins.enabled", havingValue = "true", matchIfMissing = true)
public class PluginAutoConfiguration {

    @Bean
    public PluginManager pluginManager(PluginsProperties properties) {
        return new DefaultPluginManager(Paths.get(properties.getPluginsRootFolder()));
    }

    @Bean
    public org.springframework.boot.ApplicationRunner pf4jLoader(PluginManager pluginManager) {
        return args -> {
            pluginManager.loadPlugins();
            pluginManager.startPlugins();
            System.out.println("[PF4J Starter] Plugins loaded");
        };
    }
}
