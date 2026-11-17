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
        var request = new MockClientHttpRequest(HttpMethod.GET, java.net.URI.create("http://x/y"));
        return retry.intercept(request, new byte[0], (req, body) -> {
            var response = calls.incrementAndGet() >= succeedOn
                    ? new MockClientHttpResponse(new byte[0], HttpStatus.OK)
                    : new MockClientHttpResponse(new byte[0], HttpStatus.TOO_MANY_REQUESTS);
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
}
