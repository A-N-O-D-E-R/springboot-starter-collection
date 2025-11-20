package com.anode.b2;

import com.backblaze.b2.client.B2StorageClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class B2AutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(B2AutoConfiguration.class));

    @Test
    void testAutoConfigurationLoadsWithCredentials() {
        contextRunner
            .withPropertyValues(
                "b2.enabled=true",
                "b2.applicationKeyId=test-key-id",
                "b2.applicationKey=test-key",
                "b2.defaultBucketName=test-bucket"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(B2Properties.class);
                assertThat(context).hasSingleBean(B2Service.class);

                B2Properties properties = context.getBean(B2Properties.class);
                assertThat(properties.isEnabled()).isTrue();
                assertThat(properties.getApplicationKeyId()).isEqualTo("test-key-id");
                assertThat(properties.getDefaultBucketName()).isEqualTo("test-bucket");
            });
    }

    @Test
    void testAutoConfigurationDoesNotLoadWhenDisabled() {
        contextRunner
            .withPropertyValues(
                "b2.enabled=false",
                "b2.applicationKeyId=test-key-id",
                "b2.applicationKey=test-key"
            )
            .run(context -> {
                assertThat(context).doesNotHaveBean(B2StorageClient.class);
                assertThat(context).doesNotHaveBean(B2Service.class);
            });
    }

    @Test
    void testPropertiesBinding() {
        contextRunner
            .withPropertyValues(
                "b2.enabled=true",
                "b2.applicationKeyId=my-key-id",
                "b2.applicationKey=my-secret-key",
                "b2.defaultBucketName=my-bucket",
                "b2.connectionTimeoutSeconds=45",
                "b2.socketTimeoutSeconds=90",
                "b2.maxRetries=5",
                "b2.userAgent=my-app/1.0"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(B2Properties.class);

                B2Properties properties = context.getBean(B2Properties.class);
                assertThat(properties.isEnabled()).isTrue();
                assertThat(properties.getApplicationKeyId()).isEqualTo("my-key-id");
                assertThat(properties.getApplicationKey()).isEqualTo("my-secret-key");
                assertThat(properties.getDefaultBucketName()).isEqualTo("my-bucket");
                assertThat(properties.getConnectionTimeoutSeconds()).isEqualTo(45);
                assertThat(properties.getSocketTimeoutSeconds()).isEqualTo(90);
                assertThat(properties.getMaxRetries()).isEqualTo(5);
                assertThat(properties.getUserAgent()).isEqualTo("my-app/1.0");
            });
    }

    @Test
    void testDefaultPropertiesWhenNotSpecified() {
        contextRunner
            .withPropertyValues(
                "b2.applicationKeyId=test-key-id",
                "b2.applicationKey=test-key"
            )
            .run(context -> {
                assertThat(context).hasSingleBean(B2Properties.class);

                B2Properties properties = context.getBean(B2Properties.class);
                assertThat(properties.isEnabled()).isTrue();
                assertThat(properties.getConnectionTimeoutSeconds()).isEqualTo(30);
                assertThat(properties.getSocketTimeoutSeconds()).isEqualTo(60);
                assertThat(properties.getMaxRetries()).isEqualTo(3);
            });
    }
}
