package com.anode.b2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class B2PropertiesTest {

    @Test
    void testDefaultValues() {
        B2Properties properties = new B2Properties();

        assertTrue(properties.isEnabled(), "B2 should be enabled by default");
        assertNull(properties.getApplicationKeyId(), "Application key ID should be null by default");
        assertNull(properties.getApplicationKey(), "Application key should be null by default");
        assertNull(properties.getDefaultBucketName(), "Default bucket name should be null by default");
        assertEquals(30, properties.getConnectionTimeoutSeconds(), "Default connection timeout should be 30");
        assertEquals(60, properties.getSocketTimeoutSeconds(), "Default socket timeout should be 60");
        assertEquals(3, properties.getMaxRetries(), "Default max retries should be 3");
        assertEquals("b2-spring-boot-starter/0.0.1", properties.getUserAgent(),
            "Default user agent should be set");
    }

    @Test
    void testSetEnabled() {
        B2Properties properties = new B2Properties();

        properties.setEnabled(false);
        assertFalse(properties.isEnabled(), "Enabled should be false after setting");

        properties.setEnabled(true);
        assertTrue(properties.isEnabled(), "Enabled should be true after setting");
    }

    @Test
    void testSetApplicationKeyId() {
        B2Properties properties = new B2Properties();

        String keyId = "test-key-id";
        properties.setApplicationKeyId(keyId);
        assertEquals(keyId, properties.getApplicationKeyId(),
            "Application key ID should match the set value");
    }

    @Test
    void testSetApplicationKey() {
        B2Properties properties = new B2Properties();

        String key = "test-application-key";
        properties.setApplicationKey(key);
        assertEquals(key, properties.getApplicationKey(),
            "Application key should match the set value");
    }

    @Test
    void testSetDefaultBucketName() {
        B2Properties properties = new B2Properties();

        String bucketName = "my-test-bucket";
        properties.setDefaultBucketName(bucketName);
        assertEquals(bucketName, properties.getDefaultBucketName(),
            "Default bucket name should match the set value");
    }

    @Test
    void testSetConnectionTimeoutSeconds() {
        B2Properties properties = new B2Properties();

        properties.setConnectionTimeoutSeconds(60);
        assertEquals(60, properties.getConnectionTimeoutSeconds(),
            "Connection timeout should match the set value");
    }

    @Test
    void testSetSocketTimeoutSeconds() {
        B2Properties properties = new B2Properties();

        properties.setSocketTimeoutSeconds(120);
        assertEquals(120, properties.getSocketTimeoutSeconds(),
            "Socket timeout should match the set value");
    }

    @Test
    void testSetMaxRetries() {
        B2Properties properties = new B2Properties();

        properties.setMaxRetries(5);
        assertEquals(5, properties.getMaxRetries(),
            "Max retries should match the set value");
    }

    @Test
    void testSetUserAgent() {
        B2Properties properties = new B2Properties();

        String userAgent = "custom-user-agent/1.0";
        properties.setUserAgent(userAgent);
        assertEquals(userAgent, properties.getUserAgent(),
            "User agent should match the set value");
    }
}
