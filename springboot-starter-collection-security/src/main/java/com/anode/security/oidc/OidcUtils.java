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

/**
 * Utility class for OIDC (OpenID Connect) authentication operations.
 * <p>
 * Provides helper methods for constructing OIDC authentication objects, managing user information,
 * and extracting claims from OIDC tokens. Used primarily in conjunction with OAuth2 authentication flows.
 *
 * @see org.springframework.security.oauth2.core.oidc.OidcUserInfo
 * @see org.springframework.security.core.Authentication
 */
public class OidcUtils {

    public static final Logger log = LoggerFactory.getLogger(OidcUtils.class);

    private OidcUtils() {
    }

    /**
     * Constructs an OIDC-based authentication object from user information and a claims-to-role mapper.
     * <p>
     * Creates an OAuth2 authentication token with the provided user information, automatically
     * mapping OIDC claims to granted authorities using the provided mapper.
     *
     * @param userInfo the OIDC user information containing claims
     * @param claimsToRoleMapper mapper to convert claims to Spring roles
     * @return an OAuth2AuthenticationToken ready for use in security context
     */
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

    /**
     * Creates a builder with default OIDC user information for testing or fallback scenarios.
     * <p>
     * Provides default test credentials that represent an admin user.
     *
     * @return a pre-configured OidcUserInfo builder
     */
    public static OidcUserInfo.Builder defaultUserInfoBuilder() {
        return OidcUserInfo.builder()
                .email("foo@bar.com")
                .preferredUsername("foo@bar.com")
                .givenName("Foo")
                .familyName("Bar")
                .claim(AnodeClaimsName.ROLE, "ADMIN")
                .claim(AnodeClaimsName.SITE_NAME, "Paris");
    }

    /**
     * Loads OIDC user information from a JSON file on the classpath or returns default information.
     * <p>
     * Attempts to load user-specific claims from {@code classpath:anode-test-users/{user}.json}.
     * If the file does not exist or cannot be read, returns default user information.
     *
     * @param user the username to look up
     * @return OIDC user information loaded from file or defaults
     */
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

    /**
     * Extracts a single string claim from an OIDC claims map.
     * <p>
     * Returns null if the claims map is null, the claim does not exist, or the claim value is not a string.
     *
     * @param claims the claims map (may be null)
     * @param claimName the name of the claim to extract
     * @return the claim value as a string, or null if not found or not a string
     */
    public static String getClaim(Map<String, Object> claims, String claimName) {
        if (null == claims) {
            return null;
        }
        return switch (claims.get(claimName)) {
            case String claim -> claim;
            case null, default -> null;
        };
    }

    /**
     * Extracts a claim as a list of strings from an OIDC claims map.
     * <p>
     * Attempts to extract a list of strings from the claim. If the claim does not exist, is not a list,
     * or contains non-string values, returns an empty list.
     *
     * @param claims the claims map (may be null or empty)
     * @param claimName the name of the claim to extract
     * @return the claim value as a list of strings, or an empty list if not found or invalid
     */
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
