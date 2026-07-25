package com.anode.security.oidc;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Collection;
import java.util.HashSet;

/**
 * Maps OIDC granted authorities and claims to Spring Security roles.
 * <p>
 * Processes OIDC user authorities to extract claims and converts them to Spring Security granted authorities
 * using the configured {@link ClaimsToRoleMapper}.
 */
public class AnodeGrantedAuthoritiesMapper implements GrantedAuthoritiesMapper {

    private final ClaimsToRoleMapper claimsToRoleMapper;

    /**
     * Creates a new mapper with the specified claims-to-role mapper.
     *
     * @param claimsToRoleMapper the mapper to convert claims to roles
     */
    public AnodeGrantedAuthoritiesMapper(ClaimsToRoleMapper claimsToRoleMapper) {
        this.claimsToRoleMapper = claimsToRoleMapper;
    }

    /**
     * Maps OIDC user authorities to Spring Security granted authorities.
     * <p>
     * Processes OIDC user authorities to extract their claims and maps them to roles
     * using the configured claims-to-role mapper. Non-OIDC authorities are retained as-is.
     *
     * @param authorities the authorities to map
     * @return mapped authorities including original authorities plus mapped roles
     */
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
