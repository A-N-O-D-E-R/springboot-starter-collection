package com.anode.b2;

import com.backblaze.b2.client.B2StorageClient;
import com.backblaze.b2.client.B2StorageClientFactory;
import com.backblaze.b2.client.exceptions.B2Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for Backblaze B2 integration.
 */
@Configuration
@ConditionalOnClass(B2StorageClient.class)
@EnableConfigurationProperties(B2Properties.class)
@ConditionalOnProperty(name = "b2.enabled", havingValue = "true", matchIfMissing = true)
public class B2AutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(B2AutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public B2StorageClient b2StorageClient(B2Properties properties) throws B2Exception {
        if (properties.getApplicationKeyId() == null || properties.getApplicationKey() == null) {
            throw new IllegalStateException(
                "B2 credentials not configured. Please set b2.applicationKeyId and b2.applicationKey"
            );
        }

        logger.info("Initializing B2 Storage Client");
        logger.debug("Application Key ID: {}", properties.getApplicationKeyId());
        logger.debug("User agent: {}", properties.getUserAgent());

        // Create a simple B2 client with the credentials and user agent
        B2StorageClient client = B2StorageClientFactory
                .createDefaultFactory()
                .create(properties.getApplicationKeyId(),
                       properties.getApplicationKey(),
                       properties.getUserAgent());

        logger.info("B2 Storage Client initialized successfully");

        return client;
    }

    @Bean
    @ConditionalOnMissingBean
    public B2Service b2Service(B2StorageClient client, B2Properties properties) {
        logger.info("Creating B2Service bean");
        if (properties.getDefaultBucketName() != null) {
            logger.info("Default bucket configured: {}", properties.getDefaultBucketName());
        } else {
            logger.warn("No default bucket configured. Bucket name must be specified in each operation.");
        }

        return new B2Service(client, properties);
    }
}
