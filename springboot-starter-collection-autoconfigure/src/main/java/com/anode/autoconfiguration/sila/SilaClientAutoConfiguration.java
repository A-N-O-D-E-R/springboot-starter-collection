package com.anode.autoconfiguration.sila;

import bio.anode.sila.SiLAProperties;
import bio.anode.sila.SilaClientService;
import bio.anode.sila.connection.SilaChannel;
import bio.anode.sila.connection.SilaChannelFactory;
import bio.anode.sila.connection.SilaChannelManager;
import bio.anode.sila.discovery.SilaDiscoveryService;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConditionalOnClass(ManagedChannel.class)
@EnableConfigurationProperties(SiLAProperties.class)
@ConditionalOnProperty(name = "sila.enabled", havingValue = "true", matchIfMissing = true)
public class SilaClientAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SilaClientAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public SilaChannelFactory silaChannelFactory() {
        return new SilaChannelFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public SilaChannelManager silaChannelManager(SilaChannelFactory factory,
                                                  SiLAProperties properties) {
        SilaChannelManager manager = new SilaChannelManager(factory);
        manager.initializeFromConfig(properties);
        log.info("SiLA channel manager initialized with {} server(s)",
                properties.getServers().size());
        return manager;
    }

    @Bean
    @ConditionalOnMissingBean
    public Map<String, SilaChannel> silaConnections(SilaChannelManager manager) {
        return manager.getChannels();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "sila.discovery.enabled", havingValue = "true")
    public SilaDiscoveryService silaDiscoveryService(SiLAProperties properties,
                                                      SilaChannelManager manager,
                                                      SilaChannelFactory factory) {
        SilaDiscoveryService discovery = new SilaDiscoveryService(properties.getDiscovery());

        if (properties.getDiscovery().isAutoConnect()) {
            discovery.setOnServerDiscovered(server -> {
                SilaChannel ch = factory.create(server.name(), server.host(), server.port());
                manager.register(server.name(), ch);
                log.info("Auto-connected to discovered SiLA server: {}", server.name());
            });
        }

        try {
            discovery.startDiscovery();
        } catch (Exception e) {
            log.error("Failed to start SiLA discovery", e);
        }

        return discovery;
    }

    @Bean
    @ConditionalOnMissingBean
    public SilaClientService silaClientService(SilaChannelManager manager) {
        return new SilaClientService(manager);
    }
}
