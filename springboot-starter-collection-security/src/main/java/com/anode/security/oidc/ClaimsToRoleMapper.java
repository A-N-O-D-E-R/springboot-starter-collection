package com.anode.security.oidc;

import java.util.Collection;
import java.util.Map;

@FunctionalInterface
public interface ClaimsToRoleMapper {
    Collection<String> map(Map<String, Object> claims);
}