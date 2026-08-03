package com.anode.autoconfiguration.biojava;

import com.anode.biojava.B2StructureProvider;
import com.anode.biojava.BioJavaProperties;
import com.anode.biojava.TieredStructureCache;
import org.biojava.nbio.structure.Structure;
import org.biojava.nbio.structure.align.util.AtomCache;
import org.biojava.nbio.structure.io.LocalPDBDirectory.FetchBehavior;
import org.biojava.nbio.structure.io.StructureFiletype;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.time.Duration;

@AutoConfiguration
@EnableConfigurationProperties(BioJavaProperties.class)
public class BioJavaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AtomCache atomCache(BioJavaProperties props) {
        AtomCache cache = new AtomCache();
        cache.setFiletype(StructureFiletype.CIF);
        cache.setFetchBehavior(FetchBehavior.FETCH_FILES);
        return cache;
    }

    @Bean
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnProperty(name = "biojava.cache.redis.enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public RedisConnectionFactory biojavaRedisConnectionFactory(BioJavaProperties props) {
        BioJavaProperties.Redis redis = props.getCache().getRedis();
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redis.getHost(), redis.getPort());
        config.setDatabase(redis.getDatabase());
        if (redis.getPassword() != null) {
            config.setPassword(redis.getPassword());
        }
        return new LettuceConnectionFactory(config);
    }

    @Bean
    @ConditionalOnClass(RedisConnectionFactory.class)
    @ConditionalOnProperty(name = "biojava.cache.redis.enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public RedisTemplate<String, Structure> structureRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Structure> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return template;
    }

    @Bean
    @ConditionalOnClass(S3Client.class)
    @ConditionalOnProperty(name = "biojava.cache.b2.enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public S3Client b2S3Client(BioJavaProperties props) {
        BioJavaProperties.B2 b2 = props.getCache().getB2();
        return S3Client.builder()
                .region(Region.US_WEST_2)
                .endpointOverride(URI.create(b2.getEndpoint()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(b2.getKeyId(), b2.getApplicationKey())))
                .build();
    }

    @Bean
    @ConditionalOnClass(S3Client.class)
    @ConditionalOnProperty(name = "biojava.cache.b2.enabled", havingValue = "true")
    @ConditionalOnMissingBean
    public B2StructureProvider b2StructureProvider(S3Client s3, BioJavaProperties props) {
        return new B2StructureProvider(s3, props.getCache().getB2().getBucket());
    }

    @Bean
    @ConditionalOnMissingBean
    public TieredStructureCache tieredStructureCache(
            BioJavaProperties props,
            @Autowired(required = false) RedisTemplate<String, Structure> redis,
            @Autowired(required = false) B2StructureProvider b2) {
        return new TieredStructureCache(redis, b2, props.getCache().getCaffeineMaxSize());
    }
}
