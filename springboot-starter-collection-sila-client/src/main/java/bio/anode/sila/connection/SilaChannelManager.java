package bio.anode.sila.connection;

import bio.anode.sila.SiLAProperties;
import bio.anode.sila.exception.SilaConnectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SilaChannelManager implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(SilaChannelManager.class);

    private final Map<String, SilaChannel> channels = new ConcurrentHashMap<>();
    private final SilaChannelFactory factory;

    public SilaChannelManager(SilaChannelFactory factory) {
        this.factory = factory;
    }

    public void initializeFromConfig(SiLAProperties properties) {
        for (SiLAProperties.ServerConnection serverConfig : properties.getServers()) {
            try {
                SilaChannel ch = factory.create(serverConfig);
                channels.put(serverConfig.getName(), ch);
                log.info("SiLA channel '{}' created -> {}:{}",
                        serverConfig.getName(), serverConfig.getHost(), serverConfig.getPort());
            } catch (Exception e) {
                throw new SilaConnectionException(
                        "Failed to create SiLA channel: " + serverConfig.getName(), e);
            }
        }
    }

    public void register(String name, SilaChannel channel) {
        channels.put(name, channel);
        log.info("SiLA channel '{}' registered -> {}:{}",
                name, channel.getHost(), channel.getPort());
    }

    public SilaChannel getChannel(String name) {
        SilaChannel ch = channels.get(name);
        if (ch == null) {
            throw new SilaConnectionException("Unknown SiLA server: " + name);
        }
        return ch;
    }

    public Map<String, SilaChannel> getChannels() {
        return Collections.unmodifiableMap(channels);
    }

    @Override
    public void destroy() {
        log.info("Shutting down {} SiLA channel(s)...", channels.size());
        for (Map.Entry<String, SilaChannel> entry : channels.entrySet()) {
            try {
                entry.getValue().close();
                log.info("SiLA channel '{}' closed", entry.getKey());
            } catch (Exception e) {
                log.error("Error closing SiLA channel '{}'", entry.getKey(), e);
            }
        }
        channels.clear();
    }
}
