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

/**
 * Environment post-processor that configures Kerberos authentication settings during Spring Boot startup.
 * <p>
 * This processor reads Kerberos configuration from environment variables and automatically configures
 * Spring Security's Kerberos properties. It is activated when the property {@code anode.security.env-kerberos}
 * is set to {@code true}.
 * <p>
 * Required environment variables when enabled:
 * <ul>
 *   <li>{@code KERBEROS_SERVICE_PRINCIPAL} - Kerberos service principal (e.g., HTTP/hostname@REALM)</li>
 *   <li>{@code KERBEROS_KEYTAB} - Path to the keytab file containing credentials</li>
 * </ul>
 * <p>
 * Optional environment variables:
 * <ul>
 *   <li>{@code KERBEROS_LDAP_URL} - LDAP server URL for user lookup</li>
 *   <li>{@code KERBEROS_LDAP_BASE} - LDAP search base DN</li>
 *   <li>{@code KERBEROS_LDAP_FILTER} - LDAP search filter (defaults to {@code (userPrincipalName={0})})</li>
 * </ul>
 * <p>
 * The processor runs with order {@code LOWEST_PRECEDENCE - 10} to ensure proper configuration sequence.
 *
 * @see org.springframework.boot.EnvironmentPostProcessor
 * @see org.springframework.core.Ordered
 */
public class AnodeKerberosEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final DeferredLog log = new DeferredLog();

    public static final String ENV_KERBEROS_ENABLED = "anode.security.env-kerberos";

    @Override
    public void postProcessEnvironment(
            @NonNull ConfigurableEnvironment environment,
            @Nullable SpringApplication application
    ) {
        /**
         * Checks if Kerberos is enabled and processes the environment configuration if so.
         * Replays deferred logging after application initialization.
         *
         * @param environment the configurable environment
         * @param application the Spring application (may be null)
         */
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

    /**
     * Configures Kerberos properties for Spring Security.
     * Reads Kerberos credentials and optional LDAP settings from environment variables
     * and adds them to the Spring property sources.
     *
     * @param environment the configurable environment to add properties to
     */
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

    /**
     * Returns the execution order of this post-processor.
     * Executes at {@code LOWEST_PRECEDENCE - 10} to ensure proper ordering relative to other processors.
     *
     * @return the order value
     */
    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 10;
    }
}