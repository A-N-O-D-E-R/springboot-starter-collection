package com.anode.redis;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;

/**
 * Environment post processor for Redis configuration.
 * Configures Redis from SHARED_VALKEY_URL and SHARED_VALKEY_GROUP environment variables.
 */
public class RedisEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final DeferredLog log = new DeferredLog();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        var url = environment.getProperty("SHARED_VALKEY_URL", "");
        var clientName = environment.getProperty("SHARED_VALKEY_GROUP", "");

        var redisProperties = new HashMap<String, Object>();
        if (StringUtils.isNotBlank(url)) {
            redisProperties.put("spring.data.redis.url", url);
        }
        if (StringUtils.isNotBlank(clientName)) {
            redisProperties.put("spring.data.redis.client-name", clientName);
        }

        environment.getPropertySources().addLast(new MapPropertySource("redisProperties", redisProperties));
        log.info("Redis environment initialized");

        if (application != null) {
            // This is required as EnvironmentPostProcessors are run before the logging system is initialized
            application.addInitializers(ctx -> log.replayTo(RedisEnvironmentPostProcessor.class));
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

}
