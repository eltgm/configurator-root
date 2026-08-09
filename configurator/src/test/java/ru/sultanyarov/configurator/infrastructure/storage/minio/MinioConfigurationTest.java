package ru.sultanyarov.configurator.infrastructure.storage.minio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MinioConfigurationTest {
  @Test
  void minioClient_shouldBeCreatedFromStorageProperties() {
    ComponentImageStorageProperties properties =
        new ComponentImageStorageProperties(
            "http://localhost:9000", "access-key", "secret-key", "configurator-components");

    assertThat(new MinioConfiguration().minioClient(properties)).isNotNull();
  }
}
