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

public class AnodeKerberosEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final DeferredLog log = new DeferredLog();

    public static final String ENV_KERBEROS_ENABLED = "anode.security.env-kerberos";

    @Override
    public void postProcessEnvironment(
            @NonNull ConfigurableEnvironment environment,
            @Nullable SpringApplication application
    ) {

        boolean enabled = Binder.get(environment)
                .bind(ENV_KERBEROS_ENABLED, Boolean.class)
                .orElse(false);

        if (enabled) {
            kerberosPostProcessor(environment);
        }

        if (application != null) {
            application.addInitializers(ctx ->
                log.replayTo(AnodeKerberosEnvironmentPostProcessor.class)
            );
        }
    }

    private void kerberosPostProcessor(ConfigurableEnvironment environment) {

        var principal = environment.getProperty("KERBEROS_SERVICE_PRINCIPAL");
        var keytab = environment.getProperty("KERBEROS_KEYTAB");
        var ldapUrl = environment.getProperty("KERBEROS_LDAP_URL");
        var ldapBase = environment.getProperty("KERBEROS_LDAP_BASE");

        if (principal == null || keytab == null) {
            log.warn("Kerberos not initialized, missing principal or keytab");
            return;
        }

        var props = new HashMap<String, Object>();

        props.put("spring.security.kerberos.service-principal", principal);
        props.put("spring.security.kerberos.keytab-location", keytab);

        if (ldapUrl != null) {
            props.put("spring.security.kerberos.ldap-server-url", ldapUrl);
        }

        if (ldapBase != null) {
            props.put("spring.security.kerberos.ldap-search-base", ldapBase);
        }

        props.put(
            "spring.security.kerberos.ldap-search-filter",
            environment.getProperty(
                "KERBEROS_LDAP_FILTER",
                "(userPrincipalName={0})"
            )
        );

        environment.getPropertySources().addLast(
            new MapPropertySource("kerberosProperties", props)
        );

        log.info("Kerberos environment initialized");
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 10;
    }
}