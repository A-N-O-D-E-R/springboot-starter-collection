package com.anode.github;

/**
 * A verified GitHub webhook delivery, published as a Spring application event. Parse the payload
 * with github-api: {@code GitHub.offline().parseEventPayload(new StringReader(payload),
 * GHEventPayload.PullRequest.class)}.
 *
 * @param event {@code X-GitHub-Event} (pull_request, pull_request_review, check_run...)
 * @param deliveryId {@code X-GitHub-Delivery}, unique per delivery: use it for idempotence
 * @param payload the JSON body
 */
public record GitHubWebhookEvent(String event, String deliveryId, String payload) {}
