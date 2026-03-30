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

public class AnodeCognitoEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final DeferredLog log = new DeferredLog();
    public static final String ENV_COGNITO_KEY = "anode.security.env-cognito";
    private static final String COGINTO_ISSUER_FORMAT = "https://cognito-idp.%s.amazonaws.com/%s";

    @Override
    public void postProcessEnvironment(@NonNull ConfigurableEnvironment environment, @Nullable SpringApplication application) {

        boolean cognitoEnabled = Binder.get(environment).bind(ENV_COGNITO_KEY, Boolean.class).orElse(false);

        if (cognitoEnabled) cognitoPostProcessor(environment);

        if (application != null) {
            // This is required as EnvironmentPostProcessors are run before the logging system is initialized
            application.addInitializers(ctx -> log.replayTo(AnodeCognitoEnvironmentPostProcessor.class));
        }

    }

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

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 9;
    }
}
