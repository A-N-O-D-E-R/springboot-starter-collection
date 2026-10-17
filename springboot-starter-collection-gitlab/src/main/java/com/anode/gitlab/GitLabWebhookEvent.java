package com.anode.gitlab;

import org.gitlab4j.api.webhook.Event;

/**
 * A GitLab webhook delivery whose secret token matched, published as a Spring application event.
 *
 * @param uuid {@code X-Gitlab-Event-UUID}, unique per delivery: use it for idempotence
 * @param event the payload parsed by gitlab4j ({@code MergeRequestEvent}, {@code NoteEvent},
 *     {@code PipelineEvent}...)
 */
public record GitLabWebhookEvent(String uuid, Event event) {}
