# Security Spring Boot Starter

Spring Boot starter for auto-configuring application security with support for OAuth2/OIDC (Okta, Cognito), local self-issued JWT, and Kerberos/SPNEGO authentication.

A single `SecurityFilterChain` is built. JWT-bearing requests (Okta/Cognito/local) are routed to the right `AuthenticationManager` by issuer via a `JwtIssuerAuthenticationManagerResolver` — each provider, including the local one, is just a trusted entry in that map, so any combination of them can be enabled together. Kerberos runs alongside as an independent SPNEGO filter/provider; both paths deposit a standard Spring Security `Authentication` in the `SecurityContext`.

## Usage

```xml
<dependency>
    <groupId>com.anode</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
    <version>0.0.3-SNAPSHOT</version>
</dependency>
```

## Authentication Modes

Enable any combination via configuration (each becomes a trusted issuer on the same filter chain):

| Mode | Property |
|------|----------|
| Okta OAuth2/OIDC | `anode.security.env-okta=true` |
| AWS Cognito OAuth2 | `anode.security.env-cognito=true` |
| Local JWT (dev) | `anode.security.env-local-jwt=true` |
| Mock (form login, dev) | `anode.security.mock=true` |
| Kerberos/SPNEGO | `anode.security.env-kerberos=true` |

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

The auto-configuration then wires a `JwtDecoder`/`JwtEncoder` (HMAC-SHA256) and registers `anode.security.jwt.issuer` as a trusted issuer on the shared `JwtIssuerAuthenticationManagerResolver` — same mechanism as Okta/Cognito, just with this app acting as its own issuer. The decoder rejects any token whose `iss` claim doesn't match, so tokens you mint with `JwtEncoder` must set `.issuer(...)` to that value.

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
// Using jjwt or nimbus-jose-jwt in a test — must include the issuer claim
String token = Jwts.builder()
    .subject("test-user")
    .issuer("anode-local") // must match anode.security.jwt.issuer
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

Local JWT mode uses a symmetric shared secret with no key rotation or token revocation. Use Okta, Cognito, or another OIDC provider for production deployments.

---

## Kerberos/SPNEGO (`anode.security.env-kerberos`)

Intranet SSO via a Kerberos ticket. Runs on the same `SecurityFilterChain` as any other enabled mode — it doesn't replace Okta/Cognito/mock, it complements them.

### How it works

A `SpnegoAuthenticationProcessingFilter` is inserted before Spring's basic-auth filter; a `KerberosServiceAuthenticationProvider` validates the ticket against your keytab and loads the principal via a `UserDetailsService` (override the `UserDetailsService` bean to source roles from LDAP or elsewhere — the default just grants `ROLE_USER`).

If Kerberos is the only mode enabled, unauthenticated requests get a `WWW-Authenticate: Negotiate` challenge unconditionally. If it's combined with Okta, Cognito, or mock, Spring Security's own content-negotiation split decides which challenge applies: browser navigations (`Accept: text/html`) fall through to the OIDC/mock login page, everything else (SPNEGO-capable service clients, plain API calls) gets the Negotiate challenge. No custom routing code needed — this is the same `defaultAuthenticationEntryPointFor` mechanism Spring Security uses internally to combine formLogin with httpBasic.

Either path — Kerberos ticket or OIDC token — ends up as a standard `Authentication` in the `SecurityContext`; downstream code (`@PreAuthorize`, `Authentication` injection, etc.) doesn't need to know which one was used.

### Configuration

```properties
anode.security.env-kerberos=true

# Required
KERBEROS_SERVICE_PRINCIPAL=HTTP/myapp.example.com@EXAMPLE.COM
KERBEROS_KEYTAB=/etc/krb5/myapp.keytab

# Optional — LDAP-backed UserDetailsService lookup
KERBEROS_LDAP_URL=ldap://ldap.example.com:389
KERBEROS_LDAP_BASE=dc=example,dc=com
KERBEROS_LDAP_FILTER=(userPrincipalName={0})
```

### Combining with OIDC

```properties
anode.security.env-kerberos=true
anode.security.env-okta=true
```

Desktop clients on the intranet authenticate transparently via Kerberos; everyone else gets redirected to Okta's login page.
