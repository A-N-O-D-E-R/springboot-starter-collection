package com.anode.autoconfiguration.redis;

import com.anode.redis.AwsRedisCredentialsProviderFactory;
import com.anode.redis.RedisProperties;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.data.redis.autoconfigure.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;

/**
 * Auto-configuration for Redis with AWS ElastiCache/Valkey support.
 * Configures IAM authentication for AWS Redis clusters.
 */
@AutoConfiguration
@AutoConfigureBefore(org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration.class)
@ConditionalOnClass(AwsRedisCredentialsProviderFactory.class)
@EnableConfigurationProperties(RedisProperties.class)
public class RedisAutoConfiguration {

    @Bean
    @ConditionalOnClass(name = "software.amazon.awssdk.auth.credentials.AwsCredentialsProvider")
    public AwsCredentialsProvider awsCredentialsProvider() {
        return DefaultCredentialsProvider.create();
    }

    @Bean
    @ConditionalOnClass(name = "software.amazon.awssdk.regions.providers.AwsRegionProvider")
    public AwsRegionProvider awsRegionProvider() {
        return new DefaultAwsRegionProviderChain();
    }

    @Bean
    public LettuceClientConfigurationBuilderCustomizer lettuceClientConfigurationBuilderCustomizer(
            AwsCredentialsProvider credentialsProvider,
            AwsRegionProvider regionProvider,
            RedisProperties redisProperties) {
        Region region = regionProvider.getRegion();
        var redisCredentialsProviderFactory = new AwsRedisCredentialsProviderFactory(
                credentialsProvider,
                region,
                redisProperties.getClientName());
        return builder -> builder.redisCredentialsProviderFactory(redisCredentialsProviderFactory);
    }

}
