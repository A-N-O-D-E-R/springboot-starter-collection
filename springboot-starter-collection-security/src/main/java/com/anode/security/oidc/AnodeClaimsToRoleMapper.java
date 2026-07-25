package com.anode.security.oidc;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.anode.security.oidc.AnodeClaimsName.ROLE;
import static com.anode.security.oidc.AnodeClaimsName.TITLE;
import static com.anode.security.oidc.OidcUtils.getClaim;

/**
 * Maps OIDC claims to Spring Security roles based on a hierarchy of role and title claims.
 * <p>
 * Role derivation logic:
 * <ul>
 *   <li>ROLE_MANAGER: title=Manager AND role=admin</li>
 *   <li>ROLE_OPERATOR: title=Operator (any role)</li>
 *   <li>ROLE_ADMIN: role=admin (without manager title)</li>
 *   <li>ROLE_VIEWER: all others (default)</li>
 * </ul>
 *
 * @see ClaimsToRoleMapper
 * @see AnodeClaimsName
 */
public class AnodeClaimsToRoleMapper implements ClaimsToRoleMapper {

    /**
     * Maps OIDC claims to a collection containing a single Spring Security role.
     * <p>
     * Examines the "role" and "Title" claims to determine the appropriate role,
     * with manager taking precedence over operator, which takes precedence over admin/viewer.
     *
     * @param claims the OIDC claims from the ID token
     * @return a collection containing exactly one role string
     */
    @Override
    public Collection<String> map(Map<String, Object> claims) {

        var isAdmin = "admin".equalsIgnoreCase(getClaim(claims, ROLE));

        var isManager = "Manager".equalsIgnoreCase(getClaim(claims, TITLE))
                          && isAdmin;

        var isOperator = "Operator".equalsIgnoreCase(getClaim(claims, TITLE));

        if (isManager) {
            return List.of("ROLE_MANAGER");
        }

        if (isOperator) {
            return List.of("ROLE_OPERATOR");
        }

        return List.of(isAdmin ? "ROLE_ADMIN" : "ROLE_VIEWER");
    }
}