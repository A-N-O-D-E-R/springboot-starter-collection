package com.anode.jira;

import com.anode.jira.client.ApiClient;
import com.anode.jira.client.api.IssueCommentsApi;
import com.anode.jira.client.api.IssueSearchApi;
import com.anode.jira.client.api.IssuesApi;
import com.anode.jira.client.api.MyselfApi;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.http.HttpHeaders;

/**
 * Jira Cloud REST API v3 operations used to work with issues, generated from Atlassian's OpenAPI
 * specification ({@code src/main/openapi}). Authenticates with an account email and API token,
 * and retries rate-limited (429) and unavailable (502/503/504) responses.
 */
public final class JiraClient {

    private final IssuesApi issues;
    private final IssueSearchApi search;
    private final IssueCommentsApi comments;
    private final MyselfApi myself;

    /**
     * @param baseUrl site URL, e.g. {@code https://your-domain.atlassian.net}
     * @param email account the API token belongs to
     * @param apiToken API token (https://id.atlassian.com/manage-profile/security/api-tokens)
     * @param retry when to retry transient responses
     */
    public JiraClient(String baseUrl, String email, String apiToken, RetryPolicy retry) {
        var restClient = ApiClient.buildRestClientBuilder()
                .defaultHeaders(headers -> headers.setBasicAuth(email, apiToken))
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .requestInterceptor(new TransientResponseRetry(retry))
                .build();
        var api = new ApiClient(restClient).setBasePath(baseUrl);
        this.issues = new IssuesApi(api);
        this.search = new IssueSearchApi(api);
        this.comments = new IssueCommentsApi(api);
        this.myself = new MyselfApi(api);
    }

    /** Get an issue (use expand "renderedFields" for HTML descriptions), list and apply transitions. */
    public IssuesApi issues() {
        return issues;
    }

    /** JQL search. */
    public IssueSearchApi search() {
        return search;
    }

    /** Comment issues; bodies are Atlassian Document Format, see {@link JiraDocuments}. */
    public IssueCommentsApi comments() {
        return comments;
    }

    /** The authenticated account (a cheap connectivity check). */
    public MyselfApi myself() {
        return myself;
    }
}
