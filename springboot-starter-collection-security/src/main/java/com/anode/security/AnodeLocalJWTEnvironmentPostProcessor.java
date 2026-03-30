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

public class AnodeLocalJWTEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final DeferredLog log = new DeferredLog();

    public static final String ENV_LOCAL_JWT_ENABLED = "anode.security.env-local-jwt";

    @Override
    public void postProcessEnvironment(
            @NonNull ConfigurableEnvironment environment,
            @Nullable SpringApplication application
    ) {

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

    private void localJwtPostProcessor(ConfigurableEnvironment environment) {

        var secret = environment.getProperty("JWT_SECRET", "dev-secret-key");
        var issuer = environment.getProperty("JWT_ISSUER", "anode-local");
        var expiration = environment.getProperty("JWT_EXPIRATION", "3600");

        var props = new HashMap<String, Object>();

        // Resource server (JWT validation)
        props.put("spring.security.oauth2.resourceserver.jwt.secret", secret);

        // Optional metadata (your app can use it)
        props.put("anode.security.jwt.issuer", issuer);
        props.put("anode.security.jwt.expiration", expiration);

        environment.getPropertySources().addLast(
            new MapPropertySource("localJwtProperties", props)
        );

        log.info("Local JWT environment initialized");
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 11;
    }
}