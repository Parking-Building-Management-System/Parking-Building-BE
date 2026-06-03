package com.smartpark.swp391.infrastructure.storage.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.amazonaws.services.s3.AmazonS3;
import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.storage.config.MinioStorageProperties;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class MinioStorageServiceTest {

  @Test
  void createPresignedUploadReturnsStorageNotConfiguredWhenSettingsAreMissing() {
    MinioStorageService service =
        new MinioStorageService(
            emptyS3Provider(), new MinioStorageProperties("", "", "", "minio", ""));

    assertThatThrownBy(
            () ->
                service.createPresignedUpload(
                    UUID.randomUUID(), "fire-inspections/staff-id", "photo.jpg", "image/jpeg"))
        .isInstanceOf(ApiException.class)
        .extracting("errorCode")
        .isEqualTo(ErrorCode.STORAGE_NOT_CONFIGURED);
  }

  @SuppressWarnings("unchecked")
  private ObjectProvider<AmazonS3> emptyS3Provider() {
    ObjectProvider<AmazonS3> provider = org.mockito.Mockito.mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(null);
    return provider;
  }
}
