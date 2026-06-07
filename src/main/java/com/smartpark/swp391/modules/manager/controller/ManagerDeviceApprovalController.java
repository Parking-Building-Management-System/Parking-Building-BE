package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.manager.dto.device.DeviceApprovalRequest;
import com.smartpark.swp391.modules.manager.dto.device.ManagerDeviceResponse;
import com.smartpark.swp391.modules.manager.service.ManagerDeviceApprovalService;
import com.smartpark.swp391.modules.manager.support.ManagerTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/manager")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name = "Manager Staff Devices",
    description = "PARKING_MANAGER approval APIs for staff trusted devices and kiosk binding")
public class ManagerDeviceApprovalController {

  ManagerDeviceApprovalService managerDeviceApprovalService;
  ManagerTenantContext managerTenantContext;

  @GetMapping("/device-approvals")
  @Operation(
      summary = "List pending staff device approvals",
      description =
          "Actor: PARKING_MANAGER. Lists PENDING staff device fingerprints created when staff"
              + " login is blocked by device trust. Read-only.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Pending approvals loaded"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "PARKING_MANAGER role required")
  })
  public ResponseEntity<ApiResponse<List<ManagerDeviceResponse>>> getPending(
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/device-approvals",
        managerTenantContext.call(jwt, managerDeviceApprovalService::getPendingApprovals));
  }

  @PostMapping("/device-approvals/{id}/approve")
  @Operation(
      summary = "Approve staff device",
      description =
          "Actor: PARKING_MANAGER. Approves a pending staff device, binds it to an ACTIVE kiosk,"
              + " and sets approval metadata. Staff can log in only after kiosk assignment also"
              + " matches.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Device approved"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid kiosk binding or device status"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "PARKING_MANAGER role required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Device or kiosk not found")
  })
  public ResponseEntity<ApiResponse<ManagerDeviceResponse>> approve(
      @PathVariable UUID id,
      @Valid @RequestBody DeviceApprovalRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/device-approvals/" + id + "/approve",
        managerTenantContext.call(
            jwt,
            () -> managerDeviceApprovalService.approve(id, request, extractUserId(jwt))));
  }

  @PostMapping("/device-approvals/{id}/reject")
  @Operation(summary = "Reject staff device approval request")
  public ResponseEntity<ApiResponse<ManagerDeviceResponse>> reject(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/device-approvals/" + id + "/reject",
        managerTenantContext.call(jwt, () -> managerDeviceApprovalService.reject(id)));
  }

  @PostMapping("/devices/{id}/revoke")
  @Operation(
      summary = "Revoke staff device",
      description =
          "Actor: PARKING_MANAGER. Suspends a staff trusted device and removes its kiosk trust,"
              + " blocking future staff login from that fingerprint.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Device revoked"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "PARKING_MANAGER role required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Device not found")
  })
  public ResponseEntity<ApiResponse<ManagerDeviceResponse>> revoke(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/devices/" + id + "/revoke",
        managerTenantContext.call(jwt, () -> managerDeviceApprovalService.revoke(id)));
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
