package com.smartpark.swp391.modules.admin.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.admin.dto.security.AdminDeviceResponse;
import com.smartpark.swp391.modules.admin.dto.security.AdminSessionResponse;
import com.smartpark.swp391.modules.admin.dto.security.AuditLogResponse;
import com.smartpark.swp391.modules.admin.dto.security.ForceLogoutResponse;
import com.smartpark.swp391.modules.admin.dto.security.RevokeDeviceResponse;
import com.smartpark.swp391.modules.admin.dto.security.RevokeSessionResponse;
import com.smartpark.swp391.modules.admin.dto.security.SecurityActionRequest;
import com.smartpark.swp391.modules.admin.service.AdminAuditSecurityService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Audit Security", description = "SYSTEM_ADMIN audit and security APIs")
public class AdminAuditSecurityController {

  AdminAuditSecurityService adminAuditSecurityService;

  @GetMapping("/admin/audit/logs")
  public ResponseEntity<ApiResponse<PageResponse<AuditLogResponse>>> getAuditLogs(
      @RequestParam(required = false) UUID actorId,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) String severity,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant from,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
          Instant to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ok(
        "/admin/audit/logs",
        adminAuditSecurityService.getAuditLogs(actorId, role, severity, from, to, page, size));
  }

  @GetMapping("/admin/sessions")
  public ResponseEntity<ApiResponse<PageResponse<AdminSessionResponse>>> getSessions(
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) String role,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ok(
        "/admin/sessions",
        adminAuditSecurityService.getSessions(tenantId, role, status, page, size));
  }

  @PostMapping("/admin/users/{userId}/force-logout")
  public ResponseEntity<ApiResponse<ForceLogoutResponse>> forceLogout(
      @PathVariable UUID userId, @Valid @RequestBody(required = false) SecurityActionRequest request) {
    return ok(
        "/admin/users/" + userId + "/force-logout",
        adminAuditSecurityService.forceLogout(userId, request));
  }

  @PostMapping("/admin/sessions/{sessionId}/revoke")
  public ResponseEntity<ApiResponse<RevokeSessionResponse>> revokeSession(
      @PathVariable UUID sessionId,
      @Valid @RequestBody(required = false) SecurityActionRequest request) {
    return ok(
        "/admin/sessions/" + sessionId + "/revoke",
        adminAuditSecurityService.revokeSession(sessionId, request));
  }

  @GetMapping("/admin/devices")
  public ResponseEntity<ApiResponse<PageResponse<AdminDeviceResponse>>> getDevices(
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ok(
        "/admin/devices", adminAuditSecurityService.getDevices(tenantId, status, page, size));
  }

  @PostMapping("/admin/devices/{deviceId}/revoke")
  public ResponseEntity<ApiResponse<RevokeDeviceResponse>> revokeDevice(
      @PathVariable UUID deviceId, @Valid @RequestBody(required = false) SecurityActionRequest request) {
    return ok(
        "/admin/devices/" + deviceId + "/revoke",
        adminAuditSecurityService.revokeDevice(deviceId, request));
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
