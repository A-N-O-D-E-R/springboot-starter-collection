package com.anode.autoconfiguration.github;

import com.anode.github.GitHubWebhookEvent;
import com.anode.github.WebhookSignature;
import java.nio.charset.StandardCharsets;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives GitHub webhook deliveries, rejects those without a valid signature and publishes the
 * others as {@link GitHubWebhookEvent}s. Handle them asynchronously: GitHub expects an answer
 * within 10 seconds.
 */
@RestController
class GitHubWebhookController {

    private final String secret;
    private final ApplicationEventPublisher events;

    GitHubWebhookController(String secret, ApplicationEventPublisher events) {
        this.secret = secret;
        this.events = events;
    }

    @PostMapping("${anode.github.webhook.path:/webhooks/github}")
    ResponseEntity<Void> receive(
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader("X-GitHub-Event") String event,
            @RequestHeader("X-GitHub-Delivery") String deliveryId,
            @RequestBody byte[] body) {
        if (!WebhookSignature.isValid(secret, signature, body)) {
            return ResponseEntity.status(401).build();
        }
        events.publishEvent(new GitHubWebhookEvent(event, deliveryId, new String(body, StandardCharsets.UTF_8)));
        return ResponseEntity.accepted().build();
    }
}
