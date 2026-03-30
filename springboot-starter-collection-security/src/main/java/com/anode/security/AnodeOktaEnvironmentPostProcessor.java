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

public class AnodeOktaEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final DeferredLog log = new DeferredLog();
    public static final String ENV_OKTA_KEY = "anode.security.env-okta";
    public static final String DEFAULT_OKTA_ISSUER_PROD = "https://anode.okta.com/oauth2/default";
    public static final String DEFAULT_OKTA_ISSUER_PREVIEW = "https://anode.oktapreview.com/oauth2/default";

    @Override
    public void postProcessEnvironment(@NonNull ConfigurableEnvironment environment, @Nullable SpringApplication application) {

        boolean oktaEnabled = Binder.get(environment).bind(ENV_OKTA_KEY, Boolean.class).orElse(false);

        if (oktaEnabled) oktaPostProcessor(environment);

        if (application != null) {
            // This is required as EnvironmentPostProcessors are run before the logging system is initialized
            application.addInitializers(ctx -> log.replayTo(AnodeOktaEnvironmentPostProcessor.class));
        }

    }

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

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 9;
    }
}
