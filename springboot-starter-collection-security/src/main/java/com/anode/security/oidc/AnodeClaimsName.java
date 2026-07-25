package com.anode.security.oidc;

/**
 * Constants for OIDC claim names used in token mapping and role derivation.
 * <p>
 * These constants define the standard claim names expected in OIDC ID tokens
 * from the configured identity provider.
 *
 * @see AnodeClaimsToRoleMapper
 * @see OidcUtils
 */
public final class AnodeClaimsName {
    /** Claim name for user role (e.g., "admin", "user") */
    public static final String ROLE = "role";

    /** Claim name for user location or site name */
    public static final String SITE_NAME = "Location";

    /** Claim name for user job title (e.g., "Manager", "Operator") */
    public static final String TITLE = "Title";

    private AnodeClaimsName() {}
}
