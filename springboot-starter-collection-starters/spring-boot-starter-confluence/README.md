# spring-boot-starter-confluence

Confluence Cloud REST API v2 client (spaces, pages, child pages, footer comments), configured from properties.

```yaml
anode:
  confluence:
    base-url: https://your-domain.atlassian.net
    email: bot@your-domain.org
    api-token: ${ANODE_CONFLUENCE_API_TOKEN}
    retry: { max-retries: 3 }   # 429/502/503/504, Retry-After honoured
```

Inject `com.anode.confluence.ConfluenceClient`. Markdown for Jira bodies: `JiraDocuments.markdown(...)` in the Jira starter.
