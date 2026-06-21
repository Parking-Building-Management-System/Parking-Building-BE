package com.smartpark.swp391.infrastructure.storage.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
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

  @Test
  void objectExistsEnsuresBucketAndChecksTenantObjectKey() {
    UUID tenantId = UUID.randomUUID();
    String bucket = "smartpark";
    String objectKey = "tenants/" + tenantId + "/floor-maps/demo/map.png";
    AmazonS3 s3 = org.mockito.Mockito.mock(AmazonS3.class);
    when(s3.doesBucketExistV2(bucket)).thenReturn(false);
    when(s3.doesObjectExist(bucket, objectKey)).thenReturn(true);

    MinioStorageService service =
        new MinioStorageService(
            s3Provider(s3),
            new MinioStorageProperties(
                "http://localhost:9000", "minioadmin", "minioadmin", "minio", bucket));

    assertThat(service.objectExists(tenantId, objectKey)).isTrue();
    verify(s3).createBucket(bucket);
    verify(s3).doesObjectExist(bucket, objectKey);
  }

  @Test
  void minioEndpointNormalizationRemovesTrailingSlashesBeforeClientConfigUsesIt() {
    MinioStorageProperties properties =
        new MinioStorageProperties(
            " http://localhost:9000/// ", "minioadmin", "minioadmin", "minio", "smartpark");

    assertThat(properties.normalizedEndpoint()).isEqualTo("http://localhost:9000");
    assertThat(properties.configured()).isTrue();
  }

  @SuppressWarnings("unchecked")
  private ObjectProvider<AmazonS3> emptyS3Provider() {
    ObjectProvider<AmazonS3> provider = org.mockito.Mockito.mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(null);
    return provider;
  }

  @SuppressWarnings("unchecked")
  private ObjectProvider<AmazonS3> s3Provider(AmazonS3 s3) {
    ObjectProvider<AmazonS3> provider = org.mockito.Mockito.mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(s3);
    return provider;
  }
}
