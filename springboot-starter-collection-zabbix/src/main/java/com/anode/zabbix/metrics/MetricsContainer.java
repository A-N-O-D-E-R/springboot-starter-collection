package com.anode.zabbix.metrics;

import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MetricsContainer {

    private final Map<String, MetricsProvider> container = new HashMap<>();

    public String[] listProviders() {
        return container.keySet().toArray(String[]::new);
    }

    public void addProvider(String name, MetricsProvider provider) {
        log.info("Adding Provider: {}={}", provider.getClass().getName(), name);
        container.put(name, provider);
    }

    public void addProviders(Map<String, MetricsProvider> providers) {
        if (log.isInfoEnabled()) {
            providers.forEach((name, provider) ->
                    log.info("Adding Provider: {}={}", provider.getClass().getName(), name)
            );
        }
        container.putAll(providers);
    }

    public MetricsProvider getProvider(String name) throws MetricsException {
        MetricsProvider provider = container.get(name);
        if (provider == null) {
            throw new MetricsException("No MetricsProvider with name: " + name);
        }
        return provider;
    }

    public Object getMetric(String key) throws MetricsException {
        MetricsKey metricsKey = new MetricsKey(key);
        return getProvider(metricsKey.getProvider()).getValue(metricsKey);
    }
}
