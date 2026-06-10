package com.smartpark.swp391.infrastructure.storage.service.impl;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.storage.config.MinioStorageProperties;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedDownload;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedUpload;
import com.smartpark.swp391.infrastructure.storage.service.StorageService;
import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class MinioStorageService implements StorageService {

  private static final Duration PRESIGN_TTL = Duration.ofMinutes(15);
  private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES =
      Set.of("image/png", "image/jpeg", "image/webp");

  ObjectProvider<AmazonS3> s3Provider;
  MinioStorageProperties properties;

  @Override
  public PresignedUpload createPresignedUpload(
      UUID tenantId, String folder, String fileName, String contentType) {
    requireImageContentType(contentType);
    String objectKey = buildTenantObjectKey(tenantId, folder, fileName, contentType);
    Date expiration = expiration();

    GeneratePresignedUrlRequest request =
        new GeneratePresignedUrlRequest(properties.bucket(), objectKey)
            .withMethod(HttpMethod.PUT)
            .withExpiration(expiration);
    request.setContentType(contentType);

    URL url = s3().generatePresignedUrl(request);
    return PresignedUpload.builder()
        .objectKey(objectKey)
        .uploadUrl(url.toString())
        .method("PUT")
        .headers(Map.of("Content-Type", contentType))
        .expiresInSeconds(PRESIGN_TTL.toSeconds())
        .publicUrl(publicObjectUrl(objectKey))
        .build();
  }

  @Override
  public PresignedDownload createPresignedDownload(UUID tenantId, String objectKey) {
    requireTenantObjectKey(tenantId, objectKey);
    GeneratePresignedUrlRequest request =
        new GeneratePresignedUrlRequest(properties.bucket(), objectKey)
            .withMethod(HttpMethod.GET)
            .withExpiration(expiration());

    URL url = s3().generatePresignedUrl(request);
    return PresignedDownload.builder()
        .downloadUrl(url.toString())
        .expiresInSeconds(PRESIGN_TTL.toSeconds())
        .build();
  }

  @Override
  public void uploadObject(
      UUID tenantId,
      String objectKey,
      InputStream inputStream,
      long contentLength,
      String contentType) {
    requireTenantObjectKey(tenantId, objectKey);
    requireImageContentType(contentType);

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(contentLength);
    metadata.setContentType(contentType);

    AmazonS3 s3 = s3();
    ensureBucket(s3);
    s3.putObject(new PutObjectRequest(properties.bucket(), objectKey, inputStream, metadata));
  }

  @Override
  public boolean objectExists(UUID tenantId, String objectKey) {
    requireTenantObjectKey(tenantId, objectKey);
    AmazonS3 s3 = s3();
    ensureBucket(s3);
    return s3.doesObjectExist(properties.bucket(), objectKey);
  }

  @Override
  public String publicObjectUrl(String objectKey) {
    return null;
  }

  private AmazonS3 s3() {
    if (!properties.configured()) {
      throw new ApiException(
          ErrorCode.STORAGE_NOT_CONFIGURED, "MinIO/S3 storage is not configured");
    }
    AmazonS3 s3 = s3Provider.getIfAvailable();
    if (s3 == null) {
      throw new ApiException(
          ErrorCode.STORAGE_NOT_CONFIGURED, "MinIO/S3 storage client is not available");
    }
    return s3;
  }

  private void ensureBucket(AmazonS3 s3) {
    if (!s3.doesBucketExistV2(properties.bucket())) {
      s3.createBucket(properties.bucket());
    }
  }

  private Date expiration() {
    return Date.from(Instant.now().plus(PRESIGN_TTL));
  }

  private String buildTenantObjectKey(
      UUID tenantId, String folder, String fileName, String contentType) {
    String safeFolder = sanitizeFolder(folder);
    String safeFileName = sanitizeFileName(fileName);
    requireExtensionMatchesContentType(safeFileName, contentType);
    return "tenants/" + tenantId + "/" + safeFolder + "/" + UUID.randomUUID() + "-" + safeFileName;
  }

  private String sanitizeFolder(String folder) {
    String value = folder == null || folder.isBlank() ? "uploads" : folder.trim();
    if (!value.matches("[A-Za-z0-9/_-]+") || value.contains("..") || value.startsWith("/")) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Invalid storage folder");
    }
    return value.replaceAll("/+", "/");
  }

  private String sanitizeFileName(String fileName) {
    if (fileName == null || fileName.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "File name must not be blank");
    }
    String value = fileName.trim().replace('\\', '/');
    int slashIndex = value.lastIndexOf('/');
    if (slashIndex >= 0) {
      value = value.substring(slashIndex + 1);
    }
    value = value.replaceAll("[^A-Za-z0-9._-]", "-");
    if (value.isBlank() || value.equals(".") || value.equals("..")) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Invalid file name");
    }
    return value;
  }

  private void requireImageContentType(String contentType) {
    if (contentType == null
        || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType.trim().toLowerCase(Locale.ROOT))) {
      throw new ApiException(
          ErrorCode.INVALID_INPUT, "contentType must be image/png, image/jpeg, or image/webp");
    }
  }

  private void requireExtensionMatchesContentType(String fileName, String contentType) {
    String lower = fileName.toLowerCase(Locale.ROOT);
    String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
    boolean valid =
        ("image/png".equals(normalizedContentType) && lower.endsWith(".png"))
            || ("image/jpeg".equals(normalizedContentType)
                && (lower.endsWith(".jpg") || lower.endsWith(".jpeg")))
            || ("image/webp".equals(normalizedContentType) && lower.endsWith(".webp"));
    if (!valid) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "File extension does not match contentType");
    }
  }

  private void requireTenantObjectKey(UUID tenantId, String objectKey) {
    if (objectKey == null || objectKey.isBlank()) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "objectKey must not be blank");
    }
    String prefix = "tenants/" + tenantId + "/";
    if (!objectKey.startsWith(prefix) || objectKey.contains("..")) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "Object key is outside current tenant");
    }
  }
}
