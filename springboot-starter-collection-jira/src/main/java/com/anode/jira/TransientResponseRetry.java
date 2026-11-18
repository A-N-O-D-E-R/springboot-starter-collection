package com.anode.jira;

import java.io.IOException;
import java.util.Set;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

/**
 * Retries 429/502/503/504 responses with Spring's {@link RetryTemplate}. A {@code Retry-After} header given in
 * seconds is honoured (capped at {@value #MAX_RETRY_AFTER_SECONDS}s) on top of the policy's back-off.
 * <p>
 * 429 is retried for every method: the request was refused before it was processed. 502/503/504 only for
 * idempotent methods (GET, HEAD, PUT, DELETE, OPTIONS): a gateway error on a POST may hide a request that did go
 * through, and retrying it would create the issue or the comment twice. Those are returned as they are, and the
 * caller decides what an unknown outcome means.
 */
public final class TransientResponseRetry implements ClientHttpRequestInterceptor {

    private static final Set<HttpStatus> TRANSIENT = Set.of(
            HttpStatus.TOO_MANY_REQUESTS, HttpStatus.BAD_GATEWAY, HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.GATEWAY_TIMEOUT);

    static final long MAX_RETRY_AFTER_SECONDS = 30;

    private static final Set<HttpMethod> IDEMPOTENT = Set.of(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.OPTIONS);

    private final RetryTemplate retryTemplate;

    public TransientResponseRetry(RetryPolicy policy) {
        this.retryTemplate = new RetryTemplate(policy);
    }

    /** Thrown (and retried) when the response status is transient. */
    private static final class TransientStatus extends RuntimeException {
        final HttpStatusCode status;

        TransientStatus(HttpStatusCode status) {
            super(status.toString(), null, false, false);
            this.status = status;
        }
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        try {
            return retryTemplate.execute(() -> {
                ClientHttpResponse response = execution.execute(request, body);
                HttpStatus status = HttpStatus.resolve(response.getStatusCode().value());
                if (status != null && TRANSIENT.contains(status) && (status == HttpStatus.TOO_MANY_REQUESTS || IDEMPOTENT.contains(request.getMethod()))) {
                    long wait = retryAfterSeconds(response);
                    response.close();
                    sleep(wait);
                    throw new TransientStatus(response.getStatusCode());
                }
                return response;
            });
        } catch (RetryException e) {
            if (e.getCause() instanceof TransientStatus transientStatus) {
                throw error(transientStatus.status);
            }
            if (e.getCause() instanceof IOException io) {
                throw io;
            }
            throw new IOException(e.getCause());
        }
    }

    private static long retryAfterSeconds(ClientHttpResponse response) {
        String header = response.getHeaders().getFirst("Retry-After");
        if (header == null) {
            return 0;
        }
        try {
            return Math.clamp(Long.parseLong(header.strip()), 0, MAX_RETRY_AFTER_SECONDS);
        } catch (NumberFormatException httpDate) {
            return 0; // HTTP-date form: rely on the policy's back-off
        }
    }

    private static void sleep(long seconds) {
        if (seconds > 0) {
            try {
                Thread.sleep(seconds * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static HttpStatusCodeException error(HttpStatusCode status) {
        return status.is4xxClientError()
                ? HttpClientErrorException.create(status, "Retries exhausted", null, null, null)
                : HttpServerErrorException.create(status, "Retries exhausted", null, null, null);
    }
}
