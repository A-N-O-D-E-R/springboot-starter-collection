package com.anode.autoconfiguration.zabbix;

import com.anode.zabbix.ZabbixProperties;
import com.anode.zabbix.agent.ZabbixAgent;
import com.anode.zabbix.metrics.MetricsProvider;
import com.anode.zabbix.providers.JVMMetricsProvider;
import com.anode.zabbix.sender.ZabbixSender;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Map;

@AutoConfiguration
@ConditionalOnClass(ZabbixAgent.class)
@ConditionalOnProperty(prefix = "zabbix", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ZabbixProperties.class)
public class ZabbixAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JVMMetricsProvider jvmMetricsProvider() {
        return new JVMMetricsProvider();
    }

    @Bean
    @ConditionalOnMissingBean
    public ZabbixAgent zabbixAgent(ZabbixProperties properties, Map<String, MetricsProvider> providers) throws Exception {
        ZabbixAgent agent = new ZabbixAgent();

        // Passive configuration
        ZabbixProperties.Passive passive = properties.getPassive();
        agent.setEnablePassive(passive.isEnabled());
        agent.setListenPort(passive.getListenPort());
        if (passive.getListenAddress() != null) {
            agent.setListenAddress(passive.getListenAddress());
        }

        // Active configuration
        ZabbixProperties.Active active = properties.getActive();
        agent.setEnableActive(active.isEnabled());
        agent.setHostName(active.getHostName());
        agent.setServerAddress(active.getServerAddress());
        agent.setServerPort(active.getServerPort());
        agent.setRefreshInterval(active.getRefreshInterval());
        agent.setPskIdentity(active.getPskIdentity());
        agent.setPsk(active.getPsk());

        // Register all MetricsProvider beans
        agent.setProviders(providers);

        agent.start();
        return agent;
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "zabbix.sender", name = "host")
    public ZabbixSender zabbixSender(ZabbixProperties properties) {
        ZabbixProperties.Sender sender = properties.getSender();
        return new ZabbixSender(sender.getHost(), sender.getPort(),
                sender.getConnectTimeout(), sender.getSocketTimeout());
    }
}
