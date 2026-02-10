package com.anode.zabbix.metrics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MetricsKeyTest {
	@Test
	void testSimpleMetricsKey() {
		MetricsKey key = new MetricsKey("processed-jobs.COUNTER");
		assertEquals("processed-jobs", key.getProvider());
		assertEquals("COUNTER", key.getKey());
	}

	@Test
	void testMetricsKeyWithMultipleDotProviderName() {
		MetricsKey key = new MetricsKey("com.anode.zabbix.processed-jobs.COUNTER");
		assertEquals("com.anode.zabbix.processed-jobs", key.getProvider());
		assertEquals("COUNTER", key.getKey());
	}

	@Test
	void testMetricsKeyWithMultipleDotProviderNameWithParameters() {
		MetricsKey key = new MetricsKey("com.anode.zabbix.processed-jobs.COUNTER[ a, b, c]");
		assertEquals("com.anode.zabbix.processed-jobs", key.getProvider());
		assertEquals("COUNTER", key.getKey());
		assertTrue(key.hasParameters());
		assertEquals(3, key.getParameters().length);
		assertEquals(" a", key.getParameters()[0]);
		assertEquals(" b", key.getParameters()[1]);
		assertEquals(" c", key.getParameters()[2]);
	}

	@Test
	void testMetricsKeyWithMultipleDotProviderNameWithQuotedParameters() {
		MetricsKey key = new MetricsKey("com.anode.zabbix.processed-jobs.COUNTER[\" a\",\" \"b\", c]");

		assertEquals("com.anode.zabbix.processed-jobs", key.getProvider());
		assertEquals("COUNTER", key.getKey());
		assertTrue(key.hasParameters());
		assertEquals(3, key.getParameters().length);
		assertEquals(" a", key.getParameters()[0]);
		assertEquals(" \"b", key.getParameters()[1]);
		assertEquals(" c", key.getParameters()[2]);
	}
}
