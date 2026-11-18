package com.anode.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.web.client.HttpClientErrorException;

class TransientResponseRetryTest {

    private final ClientHttpRequestInterceptor retry = new TransientResponseRetry(
            RetryPolicy.builder().maxRetries(2).delay(Duration.ofMillis(5)).build());

    private ClientHttpResponse call(AtomicInteger calls, int succeedOn, String retryAfter) throws Exception {
        return call(HttpMethod.GET, HttpStatus.TOO_MANY_REQUESTS, calls, succeedOn, retryAfter);
    }

    private ClientHttpResponse call(HttpMethod method, HttpStatus failure, AtomicInteger calls, int succeedOn, String retryAfter) throws Exception {
        var request = new MockClientHttpRequest(method, java.net.URI.create("http://x/y"));
        return retry.intercept(request, new byte[0], (req, body) -> {
            var response = calls.incrementAndGet() >= succeedOn
                    ? new MockClientHttpResponse(new byte[0], HttpStatus.OK)
                    : new MockClientHttpResponse(new byte[0], failure);
            if (retryAfter != null) {
                response.getHeaders().add("Retry-After", retryAfter);
            }
            return response;
        });
    }

    @Test
    void retriesATooManyRequestsResponseThenSucceeds() throws Exception {
        var calls = new AtomicInteger();
        assertThat(call(calls, 2, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(calls).hasValue(2);
    }

    @Test
    void honoursRetryAfterSeconds() throws Exception {
        var calls = new AtomicInteger();
        long start = System.nanoTime();
        call(calls, 2, "1");
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isGreaterThanOrEqualTo(Duration.ofMillis(900));
    }

    @Test
    void givesUpAfterTheConfiguredRetries() {
        var calls = new AtomicInteger();
        assertThatThrownBy(() -> call(calls, 99, null)).isInstanceOf(HttpClientErrorException.class);
        assertThat(calls).hasValue(3);
    }

    @Test
    void aTooManyRequestsPostIsRetriedButAGatewayErrorOnAPostIsNot() throws Exception {
        var limited = new AtomicInteger();
        assertThat(call(HttpMethod.POST, HttpStatus.TOO_MANY_REQUESTS, limited, 2, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(limited).hasValue(2);

        // 503 on a POST may have been processed: handed back untouched, once
        var unavailable = new AtomicInteger();
        assertThat(call(HttpMethod.POST, HttpStatus.SERVICE_UNAVAILABLE, unavailable, 99, null).getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(unavailable).hasValue(1);

        // the same 503 on an idempotent request is retried
        var retried = new AtomicInteger();
        assertThat(call(HttpMethod.PUT, HttpStatus.SERVICE_UNAVAILABLE, retried, 2, null).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retried).hasValue(2);
    }
}
