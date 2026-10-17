package com.anode.github;

import java.time.Instant;

/**
 * A short-lived GitHub App installation token restricted to one repository and to the requested
 * permissions, e.g. to let a process clone or push without holding the App's private key.
 *
 * @param token value to send as {@code Authorization: token ...} or as git HTTP password
 * @param expiresAt when GitHub stops accepting it (at most one hour after creation)
 */
public record RepositoryToken(String token, Instant expiresAt) {

    @Override
    public String toString() {
        return "RepositoryToken[expiresAt=" + expiresAt + "]"; // never log the token
    }
}
