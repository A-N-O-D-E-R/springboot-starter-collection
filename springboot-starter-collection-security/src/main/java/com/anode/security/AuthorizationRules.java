package com.anode.security;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

/**
 * URL authorization rules of the application, applied by the starter's filter chain after its
 * public patterns and before its final "any other request must be authenticated". Declare beans of
 * this type, e.g. {@code rules -> rules.requestMatchers("/internal/**").hasRole("WORKER")}: unlike
 * method security, they are enforced before the request body is even read.
 */
@FunctionalInterface
public interface AuthorizationRules {

    void configure(AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry rules);
}
