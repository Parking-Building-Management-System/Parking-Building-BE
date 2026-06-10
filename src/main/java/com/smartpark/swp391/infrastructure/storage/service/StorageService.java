package com.smartpark.swp391.infrastructure.storage.service;

import com.smartpark.swp391.infrastructure.storage.dto.PresignedDownload;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedUpload;
import java.io.InputStream;
import java.util.UUID;

public interface StorageService {
  PresignedUpload createPresignedUpload(
      UUID tenantId, String folder, String fileName, String contentType);

  PresignedDownload createPresignedDownload(UUID tenantId, String objectKey);

  void uploadObject(
      UUID tenantId,
      String objectKey,
      InputStream inputStream,
      long contentLength,
      String contentType);

  boolean objectExists(UUID tenantId, String objectKey);

  String publicObjectUrl(String objectKey);
}
