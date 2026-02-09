package bio.anode.sila.discovery;

import bio.anode.sila.SiLAProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceListener;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class SilaDiscoveryService implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(SilaDiscoveryService.class);

    private final SiLAProperties.Discovery config;
    private JmDNS jmDNS;
    private final List<DiscoveredServer> discoveredServers = new CopyOnWriteArrayList<>();
    private Consumer<DiscoveredServer> onServerDiscovered;

    public SilaDiscoveryService(SiLAProperties.Discovery config) {
        this.config = config;
    }

    public void startDiscovery() throws IOException {
        jmDNS = JmDNS.create();
        jmDNS.addServiceListener(config.getServiceType(), new ServiceListener() {
            @Override
            public void serviceAdded(ServiceEvent event) {
                jmDNS.requestServiceInfo(event.getType(), event.getName());
            }

            @Override
            public void serviceResolved(ServiceEvent event) {
                String[] addresses = event.getInfo().getHostAddresses();
                if (addresses.length == 0) {
                    return;
                }
                DiscoveredServer server = new DiscoveredServer(
                        event.getName(),
                        addresses[0],
                        event.getInfo().getPort()
                );
                if (matchesFilter(server)) {
                    discoveredServers.add(server);
                    log.info("Discovered SiLA server: {} at {}:{}",
                            server.name(), server.host(), server.port());
                    if (onServerDiscovered != null) {
                        onServerDiscovered.accept(server);
                    }
                }
            }

            @Override
            public void serviceRemoved(ServiceEvent event) {
                log.info("SiLA server removed: {}", event.getName());
                discoveredServers.removeIf(s -> s.name().equals(event.getName()));
            }
        });
        log.info("SiLA discovery started (serviceType={})", config.getServiceType());
    }

    public void setOnServerDiscovered(Consumer<DiscoveredServer> callback) {
        this.onServerDiscovered = callback;
    }

    public List<DiscoveredServer> getDiscoveredServers() {
        return List.copyOf(discoveredServers);
    }

    private boolean matchesFilter(DiscoveredServer server) {
        if (config.getServerNameFilter() == null) {
            return true;
        }
        return server.name().matches(config.getServerNameFilter());
    }

    @Override
    public void close() throws IOException {
        if (jmDNS != null) {
            jmDNS.close();
        }
    }
}
