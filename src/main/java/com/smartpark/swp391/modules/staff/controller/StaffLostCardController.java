package com.smartpark.swp391.modules.staff.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardCaseRequest;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardCaseResponse;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardCompleteExitRequest;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardCompleteExitResponse;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardPhotoPresignRequest;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardPhotoPresignResponse;
import com.smartpark.swp391.modules.staff.dto.lostcard.StaffLostCardPreviewResponse;
import com.smartpark.swp391.modules.staff.service.StaffLostCardService;
import com.smartpark.swp391.modules.staff.support.StaffTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
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
@RequestMapping("/staff/lost-card")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('STAFF')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Staff Lost Card", description = "STAFF lost RFID card exit APIs")
public class StaffLostCardController {

  StaffLostCardService staffLostCardService;
  StaffTenantContext staffTenantContext;

  @PostMapping("/photos/presign-upload")
  @Operation(summary = "Create presigned upload URL for lost-card evidence")
  public ResponseEntity<ApiResponse<StaffLostCardPhotoPresignResponse>> presignPhoto(
      @Valid @RequestBody StaffLostCardPhotoPresignRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/lost-card/photos/presign-upload",
        staffTenantContext.call(jwt, () -> staffLostCardService.createPhotoUpload(request)));
  }

  @GetMapping("/preview")
  @Operation(summary = "Preview active session and fines for a lost-card exit by plate")
  public ResponseEntity<ApiResponse<StaffLostCardPreviewResponse>> preview(
      @RequestParam @NotBlank String plateNumber, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/lost-card/preview",
        staffTenantContext.call(jwt, () -> staffLostCardService.previewByPlate(plateNumber)));
  }

  @PostMapping("/cases")
  @Operation(summary = "Create a lost-card penalty case")
  public ResponseEntity<ApiResponse<StaffLostCardCaseResponse>> createCase(
      @Valid @RequestBody StaffLostCardCaseRequest request, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/lost-card/cases",
        staffTenantContext.call(jwt, () -> staffLostCardService.createCase(request)));
  }

  @PostMapping("/complete-exit")
  @Operation(summary = "Complete a lost-card exit and mark the RFID card lost")
  public ResponseEntity<ApiResponse<StaffLostCardCompleteExitResponse>> completeExit(
      @Valid @RequestBody StaffLostCardCompleteExitRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/lost-card/complete-exit",
        staffTenantContext.call(jwt, () -> staffLostCardService.completeExit(request)));
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
