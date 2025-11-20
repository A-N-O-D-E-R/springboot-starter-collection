package com.anode.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = PF4JAutoConfiguration.class)
@TestPropertySource(properties = {
    "plugins.enabled=true",
    "plugins.pluginsRootFolder=${java.io.tmpdir}/test-plugins"
})
class PF4JAutoConfigurationIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired(required = false)
    private PluginManager pluginManager;

    @Autowired(required = false)
    private PluginsProperties properties;

    @Test
    void contextLoads() {
        assertThat(context).isNotNull();
    }

    @Test
    void pluginManagerBeanIsPresent() {
        assertThat(pluginManager).isNotNull();
        assertThat(context.containsBean("pluginManager")).isTrue();
    }

    @Test
    void pf4jLoaderBeanIsPresent() {
        assertThat(context.containsBean("pf4jLoader")).isTrue();
    }

    @Test
    void propertiesAreBound() {
        assertThat(properties).isNotNull();
        assertThat(properties.isEnabled()).isTrue();
        assertThat(properties.getPluginsRootFolder()).isNotEmpty();
    }

    @Test
    void pluginManagerIsConfigured() {
        assertThat(pluginManager).isNotNull();
        assertThat(pluginManager.getPlugins()).isEmpty(); // No plugins loaded in test
    }
}
