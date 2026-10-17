package com.anode.autoconfiguration.gitlab;

import com.anode.gitlab.GitLabWebhookEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.WebHookManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Receives GitLab webhook deliveries: gitlab4j checks {@code X-Gitlab-Token} and parses the event,
 * which is then published as a {@link GitLabWebhookEvent}.
 */
@RestController
class GitLabWebhookController {

    private final WebHookManager webhooks;
    private final ApplicationEventPublisher events;

    GitLabWebhookController(WebHookManager webhooks, ApplicationEventPublisher events) {
        this.webhooks = webhooks;
        this.events = events;
    }

    @PostMapping("${anode.gitlab.webhook.path:/webhooks/gitlab}")
    ResponseEntity<Void> receive(HttpServletRequest request) {
        String uuid = request.getHeader("X-Gitlab-Event-UUID");
        try {
            events.publishEvent(new GitLabWebhookEvent(uuid, webhooks.handleRequest(request)));
        } catch (GitLabApiException e) {
            // wrong or missing secret token, or a payload gitlab4j does not understand
            return ResponseEntity.status(e.getHttpStatus() == 0 ? 401 : e.getHttpStatus()).build();
        }
        return ResponseEntity.accepted().build();
    }
}
