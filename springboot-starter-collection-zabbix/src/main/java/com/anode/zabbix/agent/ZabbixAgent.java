package com.anode.zabbix.agent;

import com.anode.zabbix.agent.active.ActiveThread;
import com.anode.zabbix.agent.passive.ListenerThread;
import com.anode.zabbix.metrics.MetricsContainer;
import com.anode.zabbix.metrics.MetricsProvider;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class ZabbixAgent {

    private boolean enablePassive = true;
    private InetAddress listenAddress;
    private int listenPort = 10050;

    private boolean enableActive = false;
    private String hostName;
    private String serverAddress;
    private int serverPort = 10051;
    private int refreshInterval = 120;
    private String pskIdentity;
    private String psk;

    private final MetricsContainer metricsContainer = new MetricsContainer();
    private ListenerThread listenerThread;
    private ActiveThread activeThread;

    /**
     * Start the agent: launches passive listener and/or active agent threads.
     */
    public void start() throws Exception {
        log.info("Starting Zabbix agent.");

        if (enablePassive) {
            log.info("Starting passive listener.");
            listenerThread = (listenAddress == null)
                    ? new ListenerThread(metricsContainer, listenPort)
                    : new ListenerThread(metricsContainer, listenAddress, listenPort);
            listenerThread.start();
            log.info("Passive listener started.");
        }

        if (enableActive) {
            log.info("Starting active agent with {}", serverAddress);
            InetAddress addr = InetAddress.getByName(serverAddress);
            activeThread = new ActiveThread(metricsContainer, hostName, serverAddress,
                    serverPort, refreshInterval, pskIdentity, psk);
            activeThread.start();
            log.info("Active agent started with {}", addr);
        }

        log.info("Zabbix agent started.");
    }

    /**
     * Stop the agent: shuts down passive listener and/or active agent threads.
     */
    public void stop() {
        log.info("Stopping Zabbix agent.");

        if (enablePassive && listenerThread != null) {
            log.info("Stopping passive listener.");
            listenerThread.shutdown();
            try {
                listenerThread.join();
            } catch (InterruptedException ignored) {}
            log.info("Passive listener stopped.");
        }

        if (enableActive && activeThread != null) {
            log.info("Stopping active agent.");
            activeThread.shutdown();
            try {
                activeThread.join();
            } catch (InterruptedException ignored) {}
            log.info("Active agent stopped.");
        }

        log.info("Zabbix agent stopped.");
    }

    /** Add a single MetricsProvider. */
    public void addProvider(String name, MetricsProvider provider) {
        metricsContainer.addProvider(name, provider);
    }

    /** Add multiple MetricsProviders at once. */
    public void setProviders(Map<String, MetricsProvider> providers) {
        metricsContainer.addProviders(providers);
    }

    /** Convenience setter for listen address using a string. */
    public void setListenAddress(String listenAddress) throws UnknownHostException {
        this.listenAddress = InetAddress.getByName(listenAddress);
    }

    /** Convenience getter for listen address string. */
    public String getListenAddress() {
        return listenAddress != null ? listenAddress.getHostAddress() : null;
    }
}
