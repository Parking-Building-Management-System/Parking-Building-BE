package com.smartpark.swp391.infrastructure.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.minio")
public record MinioStorageProperties(
    String endpoint, String accessKey, String secretKey, String signingRegion, String bucket) {

  public boolean configured() {
    return hasText(endpoint) && hasText(accessKey) && hasText(secretKey) && hasText(bucket);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
