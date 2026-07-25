package com.anode.security;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.logging.DeferredLog;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.UUID;

/**
 * Environment post-processor that configures local JWT (JSON Web Token) authentication during Spring Boot startup.
 * <p>
 * This processor sets up JWT-based authentication with locally managed keys, without requiring an external
 * identity provider. It is activated when the property {@code anode.security.env-local-jwt} is set to {@code true}.
 * <p>
 * Environment variables:
 * <ul>
 *   <li>{@code JWT_SECRET} - JWT signing secret (auto-generated UUID if not provided)</li>
 *   <li>{@code JWT_ISSUER} - JWT issuer claim (defaults to "anode-local")</li>
 *   <li>{@code JWT_EXPIRATION} - Token expiration time in seconds (defaults to 3600)</li>
 * </ul>
 * <p>
 * The processor runs with order {@code LOWEST_PRECEDENCE - 11} to ensure proper configuration sequence.
 *
 * @see org.springframework.boot.EnvironmentPostProcessor
 * @see org.springframework.core.Ordered
 */
public class AnodeLocalJWTEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final DeferredLog log = new DeferredLog();

    public static final String ENV_LOCAL_JWT_ENABLED = "anode.security.env-local-jwt";

    @Override
    public void postProcessEnvironment(
            @NonNull ConfigurableEnvironment environment,
            @Nullable SpringApplication application
    ) {
        /**
         * Checks if local JWT is enabled and processes the environment configuration if so.
         * Replays deferred logging after application initialization.
         *
         * @param environment the configurable environment
         * @param application the Spring application (may be null)
         */
        boolean enabled = Binder.get(environment)
                .bind(ENV_LOCAL_JWT_ENABLED, Boolean.class)
                .orElse(false);

        if (enabled) {
            localJwtPostProcessor(environment);
        }

        if (application != null) {
            application.addInitializers(ctx ->
                log.replayTo(AnodeLocalJWTEnvironmentPostProcessor.class)
            );
        }
    }

    /**
     * Configures JWT properties for Spring Security.
     * Initializes JWT signing secret, issuer, and expiration settings, with sensible defaults
     * for development environments.
     *
     * @param environment the configurable environment to add properties to
     */
    private void localJwtPostProcessor(ConfigurableEnvironment environment) {

        var secret = environment.getProperty("JWT_SECRET", UUID.randomUUID().toString().replace("-", ""));
        var issuer = environment.getProperty("JWT_ISSUER", "anode-local");
        var expiration = environment.getProperty("JWT_EXPIRATION", "3600");

        var props = new HashMap<String, Object>();

        props.put("spring.security.oauth2.resourceserver.jwt.secret", secret);
        props.put("anode.security.jwt.issuer", issuer);
        props.put("anode.security.jwt.expiration", expiration);

        environment.getPropertySources().addLast(
            new MapPropertySource("localJwtProperties", props)
        );

        log.info("Local JWT environment initialized");
    }

    /**
     * Returns the execution order of this post-processor.
     * Executes at {@code LOWEST_PRECEDENCE - 11} to ensure proper ordering relative to other processors.
     *
     * @return the order value
     */
    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 11;
    }
}