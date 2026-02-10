package com.anode.zabbix.metrics;

public interface MetricsProvider {
	/**
	 * @param mKey a MetricsKey instance describing the metric to retrieve.
	 * @return the value of the key.
	 * @throws MetricsException when a problem is encountered retrieving a value
	 *         for the specified key; typically when a key is not found.
	 */
	public Object getValue(MetricsKey mKey) throws MetricsException;
}
