package com.anode.security;

import org.apache.commons.lang3.StringUtils;
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

/**
 * Environment post-processor that configures AWS Cognito OAuth2 settings during Spring Boot startup.
 * <p>
 * This processor reads Cognito configuration from environment variables and automatically configures
 * Spring Security's OAuth2 properties for Cognito-based authentication. It is activated when the
 * property {@code anode.security.env-cognito} is set to {@code true}.
 * <p>
 * Required environment variables when enabled:
 * <ul>
 *   <li>{@code COGNITO_REGION} - AWS region (defaults to eu-west-3)</li>
 *   <li>{@code COGNITO_POOL_ID} - Cognito User Pool ID</li>
 *   <li>{@code COGNITO_CLIENT_ID} - OAuth2 client ID</li>
 *   <li>{@code COGNITO_CLIENT_SECRET} - OAuth2 client secret</li>
 * </ul>
 * <p>
 * Optional environment variables:
 * <ul>
 *   <li>{@code anode.security.post-logout-url} - Post-logout redirect URL (defaults to FRONTEND_URL or http://localhost:5173)</li>
 * </ul>
 * <p>
 * The processor runs with order {@code LOWEST_PRECEDENCE - 9} to ensure it processes before
 * other security configurations.
 *
 * @see org.springframework.boot.EnvironmentPostProcessor
 * @see org.springframework.core.Ordered
 */
public class AnodeCognitoEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final DeferredLog log = new DeferredLog();
    public static final String ENV_COGNITO_KEY = "anode.security.env-cognito";
    private static final String COGINTO_ISSUER_FORMAT = "https://cognito-idp.%s.amazonaws.com/%s";

    @Override
    public void postProcessEnvironment(@NonNull ConfigurableEnvironment environment, @Nullable SpringApplication application) {
        /**
         * Checks if Cognito is enabled and processes the environment configuration if so.
         * Replays deferred logging after application initialization.
         *
         * @param environment the configurable environment
         * @param application the Spring application (may be null)
         */
        boolean cognitoEnabled = Binder.get(environment).bind(ENV_COGNITO_KEY, Boolean.class).orElse(false);

        if (cognitoEnabled) cognitoPostProcessor(environment);

        if (application != null) {
            application.addInitializers(ctx -> log.replayTo(AnodeCognitoEnvironmentPostProcessor.class));
        }

    }

    /**
     * Configures OAuth2 properties for AWS Cognito.
     * Reads Cognito credentials and settings from environment variables and adds them to the
     * Spring property sources for automatic Spring Security OAuth2 configuration.
     *
     * @param environment the configurable environment to add properties to
     */
    private void cognitoPostProcessor(ConfigurableEnvironment environment) {
        var region = environment.getProperty("COGNITO_REGION", "eu-west-3");
        var poolId = environment.getProperty("COGNITO_POOL_ID");
        var clientId = environment.getProperty("COGNITO_CLIENT_ID");
        var clientSecret = environment.getProperty("COGNITO_CLIENT_SECRET");
        if (poolId == null || clientId == null || clientSecret == null) {
            log.warn("cognito env not initialized, missing properties");
            return;
        }

        String issuer = String.format(COGINTO_ISSUER_FORMAT, region, poolId);
        var postLogoutUrl = StringUtils.defaultIfBlank(
                environment.getProperty("anode.security.post-logout-url"),
                "${POST_LOGOUT_URL:${FRONTEND_URL:http://localhost:5173}}");

        var cognitoProperties = new HashMap<String, Object>();
            cognitoProperties.put("spring.security.oauth2.registration.coginto.client-id", clientId);
            cognitoProperties.put("spring.security.oauth2.registration.coginto.client-secret", clientSecret);
            cognitoProperties.put("spring.security.oauth2.registration.coginto.scope", "openid,profile,offline_access");
            cognitoProperties.put("spring.security.oauth2.registration.coginto.authorization-grant-type", "authorization_code");
            cognitoProperties.put("spring.security.oauth2.registration.coginto.post-logout-url", postLogoutUrl);
            cognitoProperties.put("spring.security.oauth2.client.provider.coginto.issuer-uri", issuer);
            cognitoProperties.put("spring.security.oauth2.client.provider.coginto.user-name-attribute", "sub");
            cognitoProperties.put("spring.security.oauth2.client.provider.coginto.jwk-set-uri", issuer + "/.well-known/jwks.json");
            
        environment.getPropertySources().addAfter("oktaProperties", new MapPropertySource("cognitoProperties", cognitoProperties));
        log.info("cognito env initialized");
    }

    /**
     * Returns the execution order of this post-processor.
     * Executes at {@code LOWEST_PRECEDENCE - 9} to ensure proper ordering relative to other processors.
     *
     * @return the order value
     */
    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 9;
    }
}
