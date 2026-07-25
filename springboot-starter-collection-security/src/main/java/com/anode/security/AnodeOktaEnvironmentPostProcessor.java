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
 * Environment post-processor that configures Okta OAuth2 settings during Spring Boot startup.
 * <p>
 * This processor reads Okta configuration from environment variables and automatically configures
 * Spring Security's OAuth2 properties for Okta-based authentication. It is activated when the
 * property {@code anode.security.env-okta} is set to {@code true}.
 * <p>
 * Required environment variables when enabled:
 * <ul>
 *   <li>{@code OKTA_CLIENT_ID} - Okta OAuth2 client ID</li>
 *   <li>{@code OKTA_CLIENT_SECRET} - Okta OAuth2 client secret</li>
 * </ul>
 * <p>
 * Optional environment variables:
 * <ul>
 *   <li>{@code OKTA_ISSUER} - Okta authorization server issuer URL (defaults based on active Spring profile)</li>
 *   <li>{@code ats.security.post-logout-url} - Post-logout redirect URL (defaults to FRONTEND_URL or http://localhost:5173)</li>
 * </ul>
 * <p>
 * Profile-based defaults:
 * <ul>
 *   <li>prod profile: {@code https://anode.okta.com/oauth2/default}</li>
 *   <li>other profiles: {@code https://anode.oktapreview.com/oauth2/default}</li>
 * </ul>
 * <p>
 * The processor runs with order {@code LOWEST_PRECEDENCE - 9} to ensure it processes before
 * other security configurations.
 *
 * @see org.springframework.boot.EnvironmentPostProcessor
 * @see org.springframework.core.Ordered
 */
public class AnodeOktaEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final DeferredLog log = new DeferredLog();
    public static final String ENV_OKTA_KEY = "anode.security.env-okta";
    public static final String DEFAULT_OKTA_ISSUER_PROD = "https://anode.okta.com/oauth2/default";
    public static final String DEFAULT_OKTA_ISSUER_PREVIEW = "https://anode.oktapreview.com/oauth2/default";

    @Override
    public void postProcessEnvironment(@NonNull ConfigurableEnvironment environment, @Nullable SpringApplication application) {
        /**
         * Checks if Okta is enabled and processes the environment configuration if so.
         * Replays deferred logging after application initialization.
         *
         * @param environment the configurable environment
         * @param application the Spring application (may be null)
         */
        boolean oktaEnabled = Binder.get(environment).bind(ENV_OKTA_KEY, Boolean.class).orElse(false);

        if (oktaEnabled) oktaPostProcessor(environment);

        if (application != null) {
            application.addInitializers(ctx -> log.replayTo(AnodeOktaEnvironmentPostProcessor.class));
        }

    }

    /**
     * Configures OAuth2 properties for Okta.
     * Reads Okta credentials and settings from environment variables and adds them to the
     * Spring property sources for automatic Spring Security OAuth2 configuration.
     *
     * @param environment the configurable environment to add properties to
     */
    private void oktaPostProcessor(ConfigurableEnvironment environment) {
        var issuer = environment.getProperty("OKTA_ISSUER",
                environment.matchesProfiles("prod") ? DEFAULT_OKTA_ISSUER_PROD : DEFAULT_OKTA_ISSUER_PREVIEW);
        var clientId = environment.getProperty("OKTA_CLIENT_ID");
        var clientSecret = environment.getProperty("OKTA_CLIENT_SECRET");
        if (clientId == null || clientSecret == null) {
            log.warn("okta env not initialized, missing properties");
            return;
        }
        var postLogoutUrl = StringUtils.defaultIfBlank(
                environment.getProperty("ats.security.post-logout-url"),
                "${POST_LOGOUT_URL:${FRONTEND_URL:http://localhost:5173}}");

        var oktaProperties = new HashMap<String, Object>();
            oktaProperties.put("okta.oauth2.issuer", issuer);
            oktaProperties.put("okta.oauth2.client-id", clientId);
            oktaProperties.put("okta.oauth2.client-secret", clientSecret);
            oktaProperties.put("okta.oauth2.scopes", "openid,profile,offline_access");
            oktaProperties.put("okta.oauth2.post-logout-redirect-uri", postLogoutUrl);

        environment.getPropertySources().addLast(new MapPropertySource("oktaProperties", oktaProperties));
        log.info("okta env initialized");
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
