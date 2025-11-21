package com.anode.jdbc;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;

/**
 * Environment post processor for JDBC configuration.
 * Configures datasource from JDBC_WRAPPER_SHARED_URL environment variable.
 */
public class JdbcEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final DeferredLog log = new DeferredLog();

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        var jdbcWrapperSharedUrl = environment.getProperty("JDBC_WRAPPER_SHARED_URL", "");

        if (StringUtils.isBlank(jdbcWrapperSharedUrl)) {
            log.debug("JDBC_WRAPPER_SHARED_URL is not defined");
        } else {
            var jdbcProperties = new HashMap<String, Object>();
            if (StringUtils.isNotBlank(jdbcWrapperSharedUrl)) {
                jdbcProperties.put("spring.datasource.url", jdbcWrapperSharedUrl);
                if (StringUtils.startsWithIgnoreCase(jdbcWrapperSharedUrl, "jdbc:aws-wrapper:")) {
                    jdbcProperties.put("spring.datasource.driver-class-name", "software.amazon.jdbc.Driver");
                } else {
                    log.info("JDBC_WRAPPER_SHARED_URL does not start with 'jdbc:aws-wrapper:', skipping driver configuration");
                }

            }

            environment.getPropertySources().addLast(new MapPropertySource("awsJdbcWrapperProperties", jdbcProperties));
            log.info("Datasource configured with JDBC_WRAPPER_SHARED_URL");
        }

        if (application != null) {
            // This is required as EnvironmentPostProcessors are run before the logging system is initialized
            application.addInitializers(ctx -> log.replayTo(JdbcEnvironmentPostProcessor.class));
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE;
    }

}
