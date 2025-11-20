package com.anode.plugin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.pf4j.PluginManager;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PF4JAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(PF4JAutoConfiguration.class));

    @Test
    void testAutoConfigurationLoadsWhenEnabled(@TempDir Path tempDir) {
        contextRunner
            .withPropertyValues(
                "plugins.enabled=true",
                "plugins.pluginsRootFolder=" + tempDir.toString()
            )
            .run(context -> {
                assertThat(context).hasSingleBean(PluginManager.class);
                assertThat(context).hasSingleBean(ApplicationRunner.class);
                assertThat(context).hasBean("pluginManager");
                assertThat(context).hasBean("pf4jLoader");
            });
    }

    @Test
    void testAutoConfigurationLoadsWithDefaultProperties(@TempDir Path tempDir) {
        contextRunner
            .withPropertyValues("plugins.pluginsRootFolder=" + tempDir.toString())
            .run(context -> {
                assertThat(context).hasSingleBean(PluginManager.class);
                assertThat(context).hasSingleBean(ApplicationRunner.class);
            });
    }

    @Test
    void testAutoConfigurationDoesNotLoadWhenDisabled() {
        contextRunner
            .withPropertyValues("plugins.enabled=false")
            .run(context -> {
                assertThat(context).doesNotHaveBean(PluginManager.class);
                assertThat(context).doesNotHaveBean("pf4jLoader");
            });
    }

    @Test
    void testPluginManagerBeanCreation(@TempDir Path tempDir) {
        contextRunner
            .withPropertyValues("plugins.pluginsRootFolder=" + tempDir.toString())
            .run(context -> {
                assertThat(context).hasSingleBean(PluginManager.class);
                PluginManager pluginManager = context.getBean(PluginManager.class);
                assertThat(pluginManager).isNotNull();
            });
    }

    @Test
    void testApplicationRunnerBeanCreation(@TempDir Path tempDir) {
        contextRunner
            .withPropertyValues("plugins.pluginsRootFolder=" + tempDir.toString())
            .run(context -> {
                assertThat(context).hasSingleBean(ApplicationRunner.class);
                ApplicationRunner runner = context.getBean(ApplicationRunner.class);
                assertThat(runner).isNotNull();
            });
    }

    @Test
    void testPropertiesBinding(@TempDir Path tempDir) {
        String customPath = tempDir.toString();
        contextRunner
            .withPropertyValues(
                "plugins.enabled=true",
                "plugins.pluginsRootFolder=" + customPath
            )
            .run(context -> {
                assertThat(context).hasSingleBean(PluginsProperties.class);
                PluginsProperties properties = context.getBean(PluginsProperties.class);
                assertThat(properties.isEnabled()).isTrue();
                assertThat(properties.getPluginsRootFolder()).isEqualTo(customPath);
            });
    }
}
