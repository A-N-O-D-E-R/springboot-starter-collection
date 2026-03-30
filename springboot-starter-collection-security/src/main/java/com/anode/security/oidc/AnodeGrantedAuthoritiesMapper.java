package com.anode.security.oidc;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Collection;
import java.util.HashSet;

public class AnodeGrantedAuthoritiesMapper implements GrantedAuthoritiesMapper {

    private final ClaimsToRoleMapper claimsToRoleMapper;

    public AnodeGrantedAuthoritiesMapper(ClaimsToRoleMapper claimsToRoleMapper) {
        this.claimsToRoleMapper = claimsToRoleMapper;
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        HashSet<GrantedAuthority> mapped = new HashSet<>(authorities.size() + 1);
        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OidcUserAuthority oidcUserAuthority) {
                var claims = oidcUserAuthority.getUserInfo().getClaims();
                var roles = claimsToRoleMapper.map(claims);
                roles.forEach(role -> mapped.add(new SimpleGrantedAuthority(role)));
            }
            mapped.add(authority);
        }
        return mapped;
    }
}
