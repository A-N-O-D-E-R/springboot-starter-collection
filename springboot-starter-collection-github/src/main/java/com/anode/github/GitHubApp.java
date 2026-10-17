package com.anode.github;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.kohsuke.github.GHPermissionType;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.kohsuke.github.authorization.AppInstallationAuthorizationProvider;
import org.kohsuke.github.extras.authorization.JWTTokenProvider;

/**
 * Clients acting as a GitHub App installation. The App JWT, installation lookup and token refresh
 * are done by github-api; this class only keeps one client per repository.
 */
public class GitHubApp {

    private final String apiUrl;
    private final JWTTokenProvider appJwt;
    private final GitHub asApp;
    private final Map<String, GitHub> clientsByRepository = new ConcurrentHashMap<>();

    /**
     * @param apiUrl REST API root ({@code https://api.github.com}, or {@code https://HOST/api/v3})
     * @param appId the App id
     * @param privateKeyPem the App private key, PKCS#8 PEM ({@code openssl pkcs8 -topk8 -nocrypt}
     *     converts the PKCS#1 key GitHub hands out)
     */
    public GitHubApp(String apiUrl, String appId, String privateKeyPem) {
        this.apiUrl = apiUrl;
        try {
            this.appJwt = new JWTTokenProvider(appId, privateKeyPem);
            this.asApp = new GitHubBuilder().withEndpoint(apiUrl).withAuthorizationProvider(appJwt).build();
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Invalid GitHub App private key (PKCS#8 PEM expected)", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A client authenticated as the App installation that has access to {@code owner/repo}. */
    public GitHub forRepository(String owner, String repo) {
        return clientsByRepository.computeIfAbsent(owner + "/" + repo, key -> {
            try {
                var installation = new AppInstallationAuthorizationProvider(
                        app -> app.getInstallationByRepository(owner, repo), appJwt);
                return new GitHubBuilder().withEndpoint(apiUrl).withAuthorizationProvider(installation).build();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    /** A token limited to {@code owner/repo} and {@code permissions} (e.g. contents=WRITE). */
    public RepositoryToken repositoryToken(String owner, String repo, Map<String, GHPermissionType> permissions) {
        try {
            var token = asApp.getApp().getInstallationByRepository(owner, repo)
                    .createToken()
                    .repositories(List.of(repo))
                    .permissions(permissions)
                    .create();
            return new RepositoryToken(token.getToken(), token.getExpiresAt().toInstant());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create a token for " + owner + "/" + repo, e);
        }
    }
}
