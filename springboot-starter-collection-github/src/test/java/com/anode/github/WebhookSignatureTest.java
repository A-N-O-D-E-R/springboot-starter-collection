package com.anode.github;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class WebhookSignatureTest {

    // test vector from GitHub's "Validating webhook deliveries" documentation
    static final String SECRET = "It's a Secret to Everybody";
    static final byte[] BODY = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    static final String SIGNATURE = "sha256=757107ea0eb2509fc211221cce984b8a37570b6d7586c22c46f4379c8b043e17";

    @Test
    void acceptsGitHubsReferenceSignature() {
        assertThat(WebhookSignature.isValid(SECRET, SIGNATURE, BODY)).isTrue();
    }

    @Test
    void rejectsATamperedBodyAWrongSecretOrAMalformedHeader() {
        assertThat(WebhookSignature.isValid(SECRET, SIGNATURE, "Hello, World?".getBytes(StandardCharsets.UTF_8))).isFalse();
        assertThat(WebhookSignature.isValid("other", SIGNATURE, BODY)).isFalse();
        assertThat(WebhookSignature.isValid(SECRET, "sha1=abc", BODY)).isFalse();
        assertThat(WebhookSignature.isValid(SECRET, "sha256=not-hex", BODY)).isFalse();
        assertThat(WebhookSignature.isValid(SECRET, null, BODY)).isFalse();
        assertThat(WebhookSignature.isValid("", SIGNATURE, BODY)).isFalse();
    }
}
