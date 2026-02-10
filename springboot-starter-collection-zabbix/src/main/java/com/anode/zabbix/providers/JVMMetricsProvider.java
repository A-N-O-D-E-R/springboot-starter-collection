package com.anode.zabbix.providers;

import com.anode.zabbix.metrics.MetricsException;
import com.anode.zabbix.metrics.MetricsKey;
import com.anode.zabbix.metrics.MetricsProvider;

public class JVMMetricsProvider implements MetricsProvider {

    @Override
    public Object getValue(MetricsKey mKey) throws MetricsException {
        if (mKey == null || mKey.getKey() == null) {
            throw new MetricsException("MetricsKey or key is null");
        }

        Runtime runtime = Runtime.getRuntime();

        return switch (mKey.getKey()) {
            case "memory.free"  -> runtime.freeMemory();
            case "memory.max"   -> runtime.maxMemory();
            case "memory.total" -> runtime.totalMemory();
            case "os.name"      -> System.getProperty("os.name");
            case "os.arch"      -> System.getProperty("os.arch");
            default -> throw new MetricsException("Unknown Key: " + mKey.getKey());
        };
    }
}
