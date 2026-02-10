package com.example.zabbix.provider;

import com.anode.zabbix.metrics.MetricsException;
import com.anode.zabbix.metrics.MetricsKey;
import com.anode.zabbix.metrics.MetricsProvider;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Custom MetricsProvider that exposes application-specific metrics to Zabbix.
 *
 * Register keys like:
 *   app.requests.total
 *   app.requests.active
 *   app.uptime
 */
@Component("app")
public class AppMetricsProvider implements MetricsProvider {

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong activeRequests = new AtomicLong(0);

    @Override
    public Object getValue(MetricsKey mKey) throws MetricsException {
        return switch (mKey.getKey()) {
            case "requests.total" -> totalRequests.get();
            case "requests.active" -> activeRequests.get();
            case "uptime" -> ManagementFactory.getRuntimeMXBean().getUptime() / 1000;
            default -> throw new MetricsException("Unknown key: " + mKey.getKey());
        };
    }

    /** Call this from your request filters / interceptors */
    public void recordRequest() {
        totalRequests.incrementAndGet();
    }

    public void incrementActive() {
        activeRequests.incrementAndGet();
    }

    public void decrementActive() {
        activeRequests.decrementAndGet();
    }
}
