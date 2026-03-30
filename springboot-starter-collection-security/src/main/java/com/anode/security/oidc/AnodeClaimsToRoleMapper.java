package com.anode.security.oidc;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.anode.security.oidc.AnodeClaimsName.ROLE;
import static com.anode.security.oidc.AnodeClaimsName.TITLE;
import static com.anode.security.oidc.OidcUtils.getClaim;

public class AnodeClaimsToRoleMapper implements ClaimsToRoleMapper {

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