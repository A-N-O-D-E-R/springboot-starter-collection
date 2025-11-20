package com.anode.plugin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PluginsPropertiesTest {

    @Test
    void testDefaultValues() {
        PluginsProperties properties = new PluginsProperties();

        assertTrue(properties.isEnabled(), "Plugins should be enabled by default");
        assertEquals("plugins", properties.getPluginsRootFolder(),
            "Default plugins folder should be 'plugins'");
    }

    @Test
    void testSetEnabled() {
        PluginsProperties properties = new PluginsProperties();

        properties.setEnabled(false);
        assertFalse(properties.isEnabled(), "Enabled should be false after setting");

        properties.setEnabled(true);
        assertTrue(properties.isEnabled(), "Enabled should be true after setting");
    }

    @Test
    void testSetPluginsRootFolder() {
        PluginsProperties properties = new PluginsProperties();

        String customPath = "/custom/plugins/path";
        properties.setPluginsRootFolder(customPath);
        assertEquals(customPath, properties.getPluginsRootFolder(),
            "Plugins root folder should match the set value");
    }

    @Test
    void testSetPluginsRootFolderWithRelativePath() {
        PluginsProperties properties = new PluginsProperties();

        String relativePath = "my-plugins";
        properties.setPluginsRootFolder(relativePath);
        assertEquals(relativePath, properties.getPluginsRootFolder(),
            "Relative path should be stored correctly");
    }

    @Test
    void testSetPluginsRootFolderWithAbsolutePath() {
        PluginsProperties properties = new PluginsProperties();

        String absolutePath = "/opt/application/plugins";
        properties.setPluginsRootFolder(absolutePath);
        assertEquals(absolutePath, properties.getPluginsRootFolder(),
            "Absolute path should be stored correctly");
    }
}
