package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.identity.enumType.StaffPasswordResetStatus;
import com.smartpark.swp391.modules.identity.service.auth.StaffPasswordResetService;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerPasswordResetCompleteRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerPasswordResetRejectRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerPasswordResetResponse;
import com.smartpark.swp391.modules.manager.support.ManagerTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/manager/password-reset-requests")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Manager Staff Password Reset Requests")
public class ManagerPasswordResetRequestController {

  StaffPasswordResetService staffPasswordResetService;
  ManagerTenantContext managerTenantContext;

  @GetMapping
  @Operation(summary = "List Staff password reset requests in the current tenant")
  public ResponseEntity<ApiResponse<PageResponse<ManagerPasswordResetResponse>>> getRequests(
      @RequestParam(required = false) StaffPasswordResetStatus status,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          LocalDateTime to,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/password-reset-requests",
        managerTenantContext.call(
            jwt,
            () -> staffPasswordResetService.getRequests(status, search, from, to, page, size)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get one Staff password reset request in the current tenant")
  public ResponseEntity<ApiResponse<ManagerPasswordResetResponse>> getRequest(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/password-reset-requests/" + id,
        managerTenantContext.call(jwt, () -> staffPasswordResetService.getRequest(id)));
  }

  @PostMapping("/{id}/complete")
  @Operation(summary = "Set the Staff password and complete a pending request")
  public ResponseEntity<ApiResponse<ManagerPasswordResetResponse>> complete(
      @PathVariable UUID id,
      @Valid @RequestBody ManagerPasswordResetCompleteRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/password-reset-requests/" + id + "/complete",
        managerTenantContext.call(
            jwt, () -> staffPasswordResetService.complete(id, request, extractManagerUserId(jwt))));
  }

  @PostMapping("/{id}/reject")
  @Operation(summary = "Reject a pending Staff password reset request")
  public ResponseEntity<ApiResponse<ManagerPasswordResetResponse>> reject(
      @PathVariable UUID id,
      @Valid @RequestBody ManagerPasswordResetRejectRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/password-reset-requests/" + id + "/reject",
        managerTenantContext.call(
            jwt, () -> staffPasswordResetService.reject(id, request, extractManagerUserId(jwt))));
  }

  private UUID extractManagerUserId(Jwt jwt) {
    if (jwt == null || jwt.getClaimAsString("user_id") == null) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
    try {
      return UUID.fromString(jwt.getClaimAsString("user_id"));
    } catch (IllegalArgumentException e) {
      throw new ApiException(ErrorCode.UNAUTHENTICATED);
    }
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
