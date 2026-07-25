# Security Spring Boot Starter

Spring Boot starter for auto-configuring application security with support for OAuth2/OIDC (Okta, Cognito), Kerberos/SPNEGO, and local JWT authentication.

## Usage

```xml
<dependency>
    <groupId>com.anode</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
    <version>0.0.2-SNAPSHOT</version>
</dependency>
```

## Authentication Modes

Activate exactly one mode via configuration:

| Mode | Property |
|------|----------|
| Okta OAuth2/OIDC | `anode.security.env-okta=true` |
| AWS Cognito OAuth2 | `anode.security.env-cognito=true` |
| Kerberos/SPNEGO | `anode.security.env-kerberos=true` |
| Local JWT (dev) | `anode.security.env-local-jwt=true` |

---

## Local JWT (`anode.security.env-local-jwt`)

Intended for **local development and testing**. Configures an OAuth2 resource server that validates HMAC-SHA256 signed JWTs using a shared secret — no external identity provider required.

### How it works

When `anode.security.env-local-jwt=true`, an `EnvironmentPostProcessor` runs early in the startup lifecycle and sets:

| Property | Source | Default |
|----------|--------|---------|
| `spring.security.oauth2.resourceserver.jwt.secret` | `$JWT_SECRET` | `dev-secret-key` |
| `anode.security.jwt.issuer` | `$JWT_ISSUER` | `anode-local` |
| `anode.security.jwt.expiration` | `$JWT_EXPIRATION` | `3600` |

The auto-configuration then wires a `JwtDecoder`/`JwtEncoder` (HMAC-SHA256) and a `SecurityFilterChain` that requires a valid Bearer token on all endpoints except `/actuator/**`.

Once a request authenticates with a valid Bearer token, the resulting `SecurityContext` is cached in the default in-memory `HttpSession` (via `HttpSessionSecurityContextRepository`, `sessionCreationPolicy(IF_REQUIRED)`) — the same in-memory, session-based persistence used by Okta mode's mock login. This means subsequent requests on the same session cookie are authenticated from the session without needing to resend the Bearer token.

### Configuration

```properties
# application-local.properties
anode.security.env-local-jwt=true

# Optional overrides (env vars or properties)
JWT_SECRET=my-256-bit-secret
JWT_ISSUER=my-app
JWT_EXPIRATION=7200
```

### Generating a test token

```java
// Using jjwt or nimbus-jose-jwt in a test
String token = Jwts.builder()
    .subject("test-user")
    .issuedAt(new Date())
    .expiration(Date.from(Instant.now().plusSeconds(3600)))
    .signWith(Keys.hmacShaKeyFor("dev-secret-key".getBytes(StandardCharsets.UTF_8)))
    .compact();
```

Then call your API:

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/resource
```

### Customizing the filter chain

Register a `Customizer<HttpSecurity>` bean to extend the default security configuration:

```java
@Bean
public Customizer<HttpSecurity> mySecurityCustomizer() {
    return http -> http.headers(headers -> headers.frameOptions(FrameOptionsConfig::sameOrigin));
}
```

### Common properties

| Property | Default | Description |
|----------|---------|-------------|
| `anode.security.env-local-jwt` | `false` | Enable local JWT mode |
| `anode.security.allowed-patterns` | `[]` | Additional URL patterns to permit without authentication |

### Do not use in production

Local JWT mode uses a symmetric shared secret with no key rotation, token revocation, or issuer validation. Use Okta, Cognito, or another OIDC provider for production deployments.
