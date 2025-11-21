package com.anode.redis;

import io.lettuce.core.RedisCredentials;
import io.lettuce.core.RedisCredentialsProvider;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.lettuce.RedisCredentialsProviderFactory;
import org.springframework.lang.NonNull;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpFullRequest;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4FamilyHttpSigner;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.time.Duration;

/**
 * AWS credentials provider factory for Redis (ElastiCache/Valkey) with IAM authentication.
 * Implements token-based authentication for AWS Redis clusters.
 */
public class AwsRedisCredentialsProviderFactory implements RedisCredentialsProviderFactory {

    private final AwsCredentialsProvider credentialsProvider;
    private final Region region;
    private final String clientName;

    public AwsRedisCredentialsProviderFactory(AwsCredentialsProvider credentialsProvider, Region region, String clientName) {
        this.credentialsProvider = credentialsProvider;
        this.region = region;
        this.clientName = clientName;
    }

    @Override
    public RedisCredentialsProvider createCredentialsProvider(@NonNull RedisConfiguration redisConfiguration) {
        var userName = RedisConfiguration.getUsernameOrElse(redisConfiguration, () -> null);
        return RedisCredentialsProvider.from(() -> userName == null ? AbsentRedisCredentials.ANONYMOUS
                : RedisCredentials.just(userName, getToken(userName)));
    }

    private String getToken(String userName) {
        var authRequest = new IAMAuthTokenRequest(userName, clientName, region);
        return authRequest.toSignedRequestUri(credentialsProvider.resolveCredentials());
    }

    private record IAMAuthTokenRequest(String userId, String cacheName, Region region) {
        private static final String REQUEST_PROTOCOL = "https://";
        private static final String PARAM_ACTION = "Action";
        private static final String PARAM_USER = "User";
        private static final String ACTION_NAME = "connect";
        private static final String SERVICE_NAME = "elasticache";
        private static final Duration TOKEN_EXPIRY_DURATION_SECONDS = Duration.ofSeconds(900);

        public String toSignedRequestUri(AwsCredentials credentials) {
            var request = SdkHttpFullRequest.builder()
                    .method(SdkHttpMethod.GET)
                    .uri(URI.create("%s%s/".formatted(REQUEST_PROTOCOL, cacheName)))
                    .appendRawQueryParameter(PARAM_ACTION, ACTION_NAME)
                    .appendRawQueryParameter(PARAM_USER, userId)
                    .build();

            return AwsV4HttpSigner.create()
                    .sign(builder -> builder
                            .identity(credentials)
                            .request(request)
                            .putProperty(AwsV4HttpSigner.REGION_NAME, region.id())
                            .putProperty(AwsV4FamilyHttpSigner.SERVICE_SIGNING_NAME, SERVICE_NAME)
                            .putProperty(AwsV4FamilyHttpSigner.EXPIRATION_DURATION, TOKEN_EXPIRY_DURATION_SECONDS)
                            .putProperty(AwsV4FamilyHttpSigner.AUTH_LOCATION, AwsV4FamilyHttpSigner.AuthLocation.QUERY_STRING))
                    .request().getUri().toString().replace(REQUEST_PROTOCOL, "");

        }

    }
}
