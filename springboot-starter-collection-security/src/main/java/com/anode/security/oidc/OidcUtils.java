package com.anode.security.oidc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class OidcUtils {

    public static final Logger log = LoggerFactory.getLogger(OidcUtils.class);

    private OidcUtils() {
    }

    public static Authentication oidcAuthentication(OidcUserInfo userInfo, ClaimsToRoleMapper claimsToRoleMapper) {

        var oidcIdToken = OidcIdToken.withTokenValue("id-token").subject("client-id").build();

        var grantedAuthoritiesMapper = new AnodeGrantedAuthoritiesMapper(claimsToRoleMapper);
        var user = new DefaultOidcUser(List.of(
                new OidcUserAuthority(oidcIdToken, userInfo),
                new SimpleGrantedAuthority("SCOPE_openid"),
                new SimpleGrantedAuthority("SCOPE_profile")
        ), oidcIdToken, userInfo);

        return new OAuth2AuthenticationToken(user, grantedAuthoritiesMapper.mapAuthorities(user.getAuthorities()), "okta");
    }

    public static OidcUserInfo.Builder defaultUserInfoBuilder() {
        return OidcUserInfo.builder()
                .email("foo@bar.com")
                .preferredUsername("foo@bar.com")
                .givenName("Foo")
                .familyName("Bar")
                .claim(AnodeClaimsName.ROLE, "ADMIN")
                .claim(AnodeClaimsName.SITE_NAME, "Paris");
    }

    
    public static OidcUserInfo getOidcUserInfo(String user) {
        var resource = new ClassPathResource("anode-test-users/%s.json".formatted(user.toLowerCase()));
        if (resource.exists()) {
            var jsonMapper = new JsonMapper();
            try {
                var claims = jsonMapper.readValue(resource.getInputStream(), new TypeReference<Map<String, Object>>() {
                });
                return new OidcUserInfo(claims);
            } catch (IOException e) {
                log.error("could not read {}", resource.getFilename(), e);
            }
        }
        return defaultUserInfoBuilder().build();
    }

    public static String getClaim(Map<String, Object> claims, String claimName) {
        if (null == claims) {
            return null;
        }
        return switch (claims.get(claimName)) {
            case String claim -> claim;
            case null, default -> null;
        };
    }

    public static List<String> getClaimAsList(Map<String, Object> claims, String claimName) {
        if (null == claims || claims.isEmpty()) {
            return List.of();
        }
        if (claims.get(claimName) instanceof List<?> list) {
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .toList();
        }
        return List.of();
    }

}
