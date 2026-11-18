package com.anode.jira;

import com.anode.jira.client.ApiClient;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.anode.jira.client.api.IssueCommentsApi;
import com.anode.jira.client.api.IssueLinksApi;
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
    private final IssueLinksApi issueLinks;
    private final MyselfApi myself;

    /**
     * @param baseUrl site URL, e.g. {@code https://your-domain.atlassian.net}
     * @param email account the API token belongs to
     * @param apiToken API token (https://id.atlassian.com/manage-profile/security/api-tokens)
     * @param retry when to retry transient responses
     */
    public JiraClient(String baseUrl, String email, String apiToken, RetryPolicy retry) {
        // Unset model properties are left out, not sent as null (Jira may refuse them); a null inside a map still goes
        // out, since that is how Jira clears a field.
        var mapper = ApiClient.createDefaultMapper(null);
        mapper.setDefaultPropertyInclusion(JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS));
        var restClient = ApiClient.buildRestClientBuilder(mapper)
                .defaultHeaders(headers -> headers.setBasicAuth(email, apiToken))
                .defaultHeader(HttpHeaders.ACCEPT, "application/json")
                .requestInterceptor(new TransientResponseRetry(retry))
                .build();
        var api = new ApiClient(restClient).setBasePath(baseUrl);
        this.issues = new IssuesApi(api);
        this.search = new IssueSearchApi(api);
        this.comments = new IssueCommentsApi(api);
        this.issueLinks = new IssueLinksApi(api);
        this.myself = new MyselfApi(api);
    }

    /** Get, create (one or in bulk), edit an issue (use expand "renderedFields" for HTML descriptions), list and apply transitions. */
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

    /** Link issues (e.g. "Blocks"). */
    public IssueLinksApi issueLinks() {
        return issueLinks;
    }

    /** The authenticated account (a cheap connectivity check). */
    public MyselfApi myself() {
        return myself;
    }
}
