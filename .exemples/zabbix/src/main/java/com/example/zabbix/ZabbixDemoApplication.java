package com.example.zabbix;

import com.anode.zabbix.agent.ZabbixAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ZabbixDemoApplication {

    private static final Logger log = LoggerFactory.getLogger(ZabbixDemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(ZabbixDemoApplication.class, args);
    }

    @Bean
    CommandLineRunner demo(ZabbixAgent agent) {
        return args -> {
            log.info("Zabbix agent started successfully.");
            log.info("Passive mode: {}", agent.isEnablePassive() ? "enabled (port " + agent.getListenPort() + ")" : "disabled");
            log.info("Active mode: {}", agent.isEnableActive() ? "enabled" : "disabled");
            log.info("Registered providers: {}", String.join(", ", agent.getMetricsContainer().listProviders()));
        };
    }
}
