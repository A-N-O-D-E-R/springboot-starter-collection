# Spring Boot Starters Collection

Spring Boot 4.1 starters (Java 25) under `io.github.a-n-o-d-e-r`. Each starter brings its feature
module and the shared auto-configuration; every feature can be switched off with
`<prefix>.enabled=false` and every bean can be replaced by declaring your own.

Import the BOM to align versions:

```xml
<dependency>
    <groupId>io.github.a-n-o-d-e-r</groupId>
    <artifactId>springboot-starter-collection-dependencies</artifactId>
    <version>0.1.0</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

| Starter | What it configures |
|---|---|
| `spring-boot-starter-security` | One security filter chain: OIDC login (Okta, Cognito, any provider), self-issued JWTs, mock login, Kerberos; roles from groups; CSRF for sessions |
| `spring-boot-starter-github` | GitHub App clients per repository, scoped installation tokens, verified webhooks |
| `spring-boot-starter-gitlab` | gitlab4j `GitLabApi`, verified webhooks |
| `spring-boot-starter-jira` | Jira Cloud client generated from Atlassian's OpenAPI spec (issues, JQL, comments, transitions) |
| `spring-boot-starter-notion` | notion-sdk-jvm `NotionClient` |
| `spring-boot-starter-logging` | Business-event logging (markers, JSON/XML encoders, archive) |
| `spring-boot-starter-jdbc` | AWS Advanced JDBC Wrapper from `JDBC_WRAPPER_SHARED_URL` |
| `spring-boot-starter-redis` | Redis with AWS IAM credentials, two-level cache |
| `spring-boot-starter-b2` | Backblaze B2 client |
| `spring-boot-starter-plugin` | PF4J plugin manager |
| `spring-boot-starter-modbus` | Modbus master |

Secrets are read from the environment only (`ANODE_GITHUB_PRIVATE_KEY`, `ANODE_JIRA_API_TOKEN`...,
through Spring's relaxed binding); none is ever logged.

## Security — `anode.security.*`

Exactly one mode family must be enabled; nothing is configured otherwise.

| Property | Meaning |
|---|---|
| `env-oidc` | Any OpenID Connect provider (Pocket ID, Keycloak...) configured through `spring.security.oauth2.client.registration.<oidc.registration-id>` / `provider.*`; its bearer tokens are also accepted when the provider has an `issuer-uri` |
| `env-okta`, `env-cognito` | Okta / Cognito login (environment post-processors read `OKTA_*`, Cognito variables) |
| `env-local-jwt` | Bearer JWTs signed by the application (HS256, `spring.security.oauth2.resourceserver.jwt.secret`, issuer `anode.security.jwt.issuer`); a `JwtEncoder` bean is provided to issue them, and every `OAuth2TokenValidator<Jwt>` bean is applied (e.g. revocation) |
| `mock` | Form login accepting any user, claims from `classpath:anode-test-users/<user>.json` — local development only |
| `env-kerberos` | SPNEGO |
| `roles.claim` / `roles.mapping.<ROLE>` | Roles granted from the groups claim, e.g. `roles.mapping.ROLE_ADMIN=factory-admins`; without mapping the historical `role`/`Title` claim mapping applies |
| `allowed-patterns` | Paths open without authentication (`/actuator/**` always is) |

Browser logins (OIDC, Okta, Cognito, mock) are session-based and protected against CSRF with the
SPA setup (`XSRF-TOKEN` cookie echoed as `X-XSRF-TOKEN`); requests carrying a bearer token are
exempt.

## GitHub — `anode.github.*`

| Property | Default | Meaning |
|---|---|---|
| `app-id`, `private-key` | — | GitHub App id and private key (PKCS#8 PEM: `openssl pkcs8 -topk8 -nocrypt -in key.pem`) |
| `api-url` | `https://api.github.com` | `https://HOST/api/v3` for GitHub Enterprise Server |
| `webhook.secret` | — | Enables the webhook endpoint, verifying `X-Hub-Signature-256` |
| `webhook.path` | `/webhooks/github` | Endpoint path (add it to `anode.security.allowed-patterns`) |

`GitHubApp.forRepository(owner, repo)` returns a github-api client acting as the installation;
`repositoryToken(owner, repo, permissions)` mints a token limited to that repository (e.g. for git
over HTTPS). Deliveries are published as `GitHubWebhookEvent` (use `deliveryId` for idempotence).

## GitLab — `anode.gitlab.*`

`url` (default `https://gitlab.com`), `token`, `webhook.secret`, `webhook.path`
(`/webhooks/gitlab`). Deliveries whose `X-Gitlab-Token` matches are parsed by gitlab4j and
published as `GitLabWebhookEvent`.

## Jira — `anode.jira.*`

`base-url` (`https://your-domain.atlassian.net`), `email`, `api-token`, `retry.max-retries` (3),
`retry.delay` (1s), `retry.max-delay` (10s). `JiraClient` exposes the generated `IssuesApi`,
`IssueSearchApi`, `IssueCommentsApi` and `MyselfApi`; `JiraDocuments.text(...)` builds comment
bodies. The operations are generated from `springboot-starter-collection-jira/src/main/openapi`;
run `filter-spec.py` against Atlassian's spec to add one.

## Notion — `anode.notion.*`

`token`, `base-url` (`https://api.notion.com/v1`).

## Build

```bash
mvn verify      # JDK 25
```

Releases: tag `v<version>`; GitHub Actions signs and publishes to Maven Central.
