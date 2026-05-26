package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffCreateRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffPasswordResetRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffResponse;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffStatusRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffUpdateRequest;
import com.smartpark.swp391.modules.manager.service.ManagerStaffService;
import com.smartpark.swp391.modules.manager.support.ManagerTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/manager/staff")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Manager Staff Accounts", description = "PARKING_MANAGER staff account APIs")
public class ManagerStaffController {

  ManagerStaffService managerStaffService;
  ManagerTenantContext managerTenantContext;

  @GetMapping
  @Operation(
      summary = "List staff accounts",
      description = "Lists STAFF users in the authenticated manager tenant.",
      responses =
          @io.swagger.v3.oas.annotations.responses.ApiResponse(
              responseCode = "200",
              content = @Content(schema = @Schema(implementation = PageResponse.class))))
  public ResponseEntity<ApiResponse<PageResponse<ManagerStaffResponse>>> getStaff(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) UserStatus status,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/staff",
        managerTenantContext.call(
            jwt, () -> managerStaffService.getStaff(search, status, page, size)));
  }

  @PostMapping
  @Operation(
      summary = "Create staff account",
      description = "Creates a STAFF user in the authenticated manager tenant without binding a device.")
  public ResponseEntity<ApiResponse<ManagerStaffResponse>> createStaff(
      @Valid @RequestBody ManagerStaffCreateRequest request, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/staff",
        managerTenantContext.call(
            jwt, () -> managerStaffService.createStaff(request, extractUserId(jwt))));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get staff account", description = "Gets one STAFF user in current tenant.")
  public ResponseEntity<ApiResponse<ManagerStaffResponse>> getStaffById(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/staff/" + id,
        managerTenantContext.call(jwt, () -> managerStaffService.getStaffById(id)));
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "Update staff account",
      description = "Updates basic staff profile fields; password is not updated here.")
  public ResponseEntity<ApiResponse<ManagerStaffResponse>> updateStaff(
      @PathVariable UUID id,
      @Valid @RequestBody ManagerStaffUpdateRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/staff/" + id,
        managerTenantContext.call(jwt, () -> managerStaffService.updateStaff(id, request)));
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Update staff status", description = "Activates or deactivates a STAFF user.")
  public ResponseEntity<ApiResponse<ManagerStaffResponse>> updateStatus(
      @PathVariable UUID id,
      @Valid @RequestBody ManagerStaffStatusRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/staff/" + id + "/status",
        managerTenantContext.call(jwt, () -> managerStaffService.updateStatus(id, request)));
  }

  @PostMapping("/{id}/reset-password")
  @Operation(
      summary = "Reset staff password",
      description = "Resets staff password and revokes active staff sessions.")
  public ResponseEntity<ApiResponse<ManagerStaffResponse>> resetPassword(
      @PathVariable UUID id,
      @Valid @RequestBody ManagerStaffPasswordResetRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/staff/" + id + "/reset-password",
        managerTenantContext.call(jwt, () -> managerStaffService.resetPassword(id, request)));
  }

  private UUID extractUserId(Jwt jwt) {
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
