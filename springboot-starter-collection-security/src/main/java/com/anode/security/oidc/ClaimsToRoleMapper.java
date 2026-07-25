package com.anode.security.oidc;

import java.util.Collection;
import java.util.Map;

/**
 * Functional interface for mapping OIDC claims to Spring Security roles.
 * <p>
 * Implementations of this interface define how OIDC ID token claims are converted
 * to Spring Security granted authority roles. This allows for flexible role derivation
 * based on custom claim structures from different identity providers.
 *
 * @see AnodeClaimsToRoleMapper
 * @see AnodeGrantedAuthoritiesMapper
 */
@FunctionalInterface
public interface ClaimsToRoleMapper {
    /**
     * Maps OIDC claims to a collection of Spring Security role strings.
     *
     * @param claims the OIDC claims from the ID token
     * @return a collection of role strings (e.g., "ROLE_ADMIN", "ROLE_USER")
     */
    Collection<String> map(Map<String, Object> claims);
}