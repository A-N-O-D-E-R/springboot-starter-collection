package com.anode.github;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** Verifies the {@code X-Hub-Signature-256} header GitHub sends with every webhook delivery. */
public final class WebhookSignature {

    private static final String PREFIX = "sha256=";

    private WebhookSignature() {}

    /** Whether {@code header} is the HMAC-SHA256 of {@code body} under {@code secret} (constant time). */
    public static boolean isValid(String secret, String header, byte[] body) {
        if (secret == null || secret.isEmpty() || header == null || !header.startsWith(PREFIX)) {
            return false;
        }
        byte[] expected;
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            expected = mac.doFinal(body);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
        byte[] received;
        try {
            received = HexFormat.of().parseHex(header.substring(PREFIX.length()));
        } catch (IllegalArgumentException e) {
            return false;
        }
        return MessageDigest.isEqual(expected, received);
    }
}
