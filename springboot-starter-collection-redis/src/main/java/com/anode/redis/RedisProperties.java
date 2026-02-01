package com.anode.redis;

import org.springframework.boot.context.properties.ConfigurationProperties;
/**
 * This replace the old RedisProperties from spring-boot-3
 */
@ConfigurationProperties(prefix = "spring.data.redis")
public class RedisProperties {
 
     private String clientName;

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
}
