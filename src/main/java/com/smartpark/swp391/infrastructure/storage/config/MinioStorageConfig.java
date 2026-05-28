package com.smartpark.swp391.infrastructure.storage.config;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@EnableConfigurationProperties(MinioStorageProperties.class)
public class MinioStorageConfig {

  MinioStorageProperties properties;

  @Bean
  @ConditionalOnExpression(
      "'${app.storage.minio.endpoint:}' != '' "
          + "&& '${app.storage.minio.access-key:}' != '' "
          + "&& '${app.storage.minio.secret-key:}' != '' "
          + "&& '${app.storage.minio.bucket:}' != ''")
  AmazonS3 minioS3Client() {
    return AmazonS3ClientBuilder.standard()
        .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration(
                properties.endpoint(), properties.signingRegion()))
        .withCredentials(
            new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(properties.accessKey(), properties.secretKey())))
        .withPathStyleAccessEnabled(true)
        .disableChunkedEncoding()
        .build();
  }

  @Bean
  ApplicationListener<ApplicationReadyEvent> minioBucketInitializer(
      ObjectProvider<AmazonS3> s3Provider) {
    return event -> {
      AmazonS3 s3 = s3Provider.getIfAvailable();
      if (s3 == null || !properties.configured()) {
        log.info(
            "MinIO/S3 storage is not configured; presign APIs will reject storage operations.");
        return;
      }

      String bucket = properties.bucket();
      try {
        if (!s3.doesBucketExistV2(bucket)) {
          s3.createBucket(bucket);
          log.info("Created MinIO/S3 bucket '{}'", bucket);
        }
      } catch (RuntimeException e) {
        log.warn("Could not verify/create MinIO/S3 bucket '{}': {}", bucket, e.getMessage());
      }
    };
  }
}
