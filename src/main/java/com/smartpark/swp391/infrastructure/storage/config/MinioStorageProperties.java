package com.smartpark.swp391.infrastructure.storage.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage.minio")
public record MinioStorageProperties(
    String endpoint, String accessKey, String secretKey, String signingRegion, String bucket) {

  public boolean configured() {
    return hasText(normalizedEndpoint())
        && hasText(accessKey)
        && hasText(secretKey)
        && hasText(bucket);
  }

  public String normalizedEndpoint() {
    if (endpoint == null) {
      return null;
    }
    String normalized = endpoint.trim();
    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
