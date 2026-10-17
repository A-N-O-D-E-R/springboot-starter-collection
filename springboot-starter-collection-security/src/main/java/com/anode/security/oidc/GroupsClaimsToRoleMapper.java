package com.anode.security.oidc;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maps the groups an identity provider puts in a claim (OIDC {@code groups}, a JWT claim...) to
 * application roles: {@code ROLE_ADMIN -> [factory-admins]} grants {@code ROLE_ADMIN} to members of
 * {@code factory-admins}. Users in no mapped group get no role.
 */
public class GroupsClaimsToRoleMapper implements ClaimsToRoleMapper {

    private final String claim;
    private final Map<String, List<String>> groupsByRole;

    public GroupsClaimsToRoleMapper(String claim, Map<String, List<String>> groupsByRole) {
        this.claim = claim;
        this.groupsByRole = Map.copyOf(groupsByRole);
    }

    @Override
    public Collection<String> map(Map<String, Object> claims) {
        Set<String> groups = Set.copyOf(OidcUtils.getClaimAsList(claims, claim));
        Set<String> roles = new LinkedHashSet<>();
        groupsByRole.forEach((role, roleGroups) -> {
            if (roleGroups.stream().anyMatch(groups::contains)) {
                roles.add(role);
            }
        });
        return roles;
    }
}
