package ru.sultanyarov.configurator.infrastructure.storage.minio;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MinioConfigurationTest {
    @Test
    void minioClient_shouldBeCreatedFromStorageProperties() {
        ComponentImageStorageProperties properties = new ComponentImageStorageProperties(
                "http://localhost:9000",
                "access-key",
                "secret-key",
                "configurator-components",
                "http://localhost:9000"
        );

        assertThat(new MinioConfiguration().minioClient(properties)).isNotNull();
    }
}
