package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedDownload;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedUpload;
import com.smartpark.swp391.infrastructure.storage.service.StorageService;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.manager.dto.storage.PresignDownloadResponse;
import com.smartpark.swp391.modules.manager.dto.storage.PresignUploadRequest;
import com.smartpark.swp391.modules.manager.dto.storage.PresignUploadResponse;
import com.smartpark.swp391.modules.manager.support.ManagerTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/manager/storage")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Manager Storage", description = "PARKING_MANAGER presigned storage APIs")
public class ManagerStorageController {

  StorageService storageService;
  ManagerTenantContext managerTenantContext;

  @PostMapping("/presign-upload")
  @Operation(summary = "Create presigned upload URL")
  public ResponseEntity<ApiResponse<PresignUploadResponse>> presignUpload(
      @Valid @RequestBody PresignUploadRequest request, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/storage/presign-upload",
        managerTenantContext.call(jwt, () -> toUploadResponse(createUpload(request))));
  }

  @GetMapping("/presign-download")
  @Operation(summary = "Create presigned download URL")
  public ResponseEntity<ApiResponse<PresignDownloadResponse>> presignDownload(
      @RequestParam String objectKey, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/storage/presign-download",
        managerTenantContext.call(jwt, () -> toDownloadResponse(createDownload(objectKey))));
  }

  private PresignedUpload createUpload(PresignUploadRequest request) {
    return storageService.createPresignedUpload(
        currentTenantId(), request.folder(), request.fileName(), request.contentType());
  }

  private PresignedDownload createDownload(String objectKey) {
    return storageService.createPresignedDownload(currentTenantId(), objectKey);
  }

  private UUID currentTenantId() {
    return TenantContext.getTenantId().orElseThrow();
  }

  private PresignUploadResponse toUploadResponse(PresignedUpload upload) {
    return PresignUploadResponse.builder()
        .objectKey(upload.objectKey())
        .uploadUrl(upload.uploadUrl())
        .method(upload.method())
        .headers(upload.headers())
        .expiresInSeconds(upload.expiresInSeconds())
        .publicUrl(upload.publicUrl())
        .build();
  }

  private PresignDownloadResponse toDownloadResponse(PresignedDownload download) {
    return PresignDownloadResponse.builder()
        .downloadUrl(download.downloadUrl())
        .expiresInSeconds(download.expiresInSeconds())
        .build();
  }

  private <T> ResponseEntity<ApiResponse<T>> ok(String path, T result) {
    return ResponseEntity.ok(
        ApiResponse.<T>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(result)
            .timestamp(Instant.now())
            .path(path)
            .build());
  }
}
