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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(
    name = "System Admin Audit & Security",
    description = "SYSTEM_ADMIN audit log, session, and trusted-device supervision APIs")
public class AdminAuditSecurityController {

  AdminAuditSecurityService adminAuditSecurityService;

  @GetMapping("/admin/audit/logs")
  @Operation(
      summary = "List audit logs",
      description =
          "Actor: SYSTEM_ADMIN. Reviews security and business audit events with optional actor,"
              + " role, severity, and time filters. Read-only; no DB state changes.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Audit logs loaded",
        content = @Content(schema = @Schema(implementation = PageResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid filter or pagination value"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required")
  })
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
  @Operation(
      summary = "List active and revoked sessions",
      description =
          "Actor: SYSTEM_ADMIN. Inspects user sessions by tenant, role, or status for monitoring"
              + " and incident response. Read-only; session revocation uses a separate endpoint.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Sessions loaded",
        content = @Content(schema = @Schema(implementation = PageResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid filter or pagination value"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required")
  })
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
  @Operation(
      summary = "Force logout all user sessions",
      description =
          "Actor: SYSTEM_ADMIN. Revokes every active session for the target user and invalidates"
              + " cached session authorization. Use when an account is compromised or suspended.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "User sessions revoked",
        content = @Content(schema = @Schema(implementation = ForceLogoutResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
  })
  public ResponseEntity<ApiResponse<ForceLogoutResponse>> forceLogout(
      @PathVariable UUID userId, @Valid @RequestBody(required = false) SecurityActionRequest request) {
    return ok(
        "/admin/users/" + userId + "/force-logout",
        adminAuditSecurityService.forceLogout(userId, request));
  }

  @PostMapping("/admin/sessions/{sessionId}/revoke")
  @Operation(
      summary = "Revoke one session",
      description =
          "Actor: SYSTEM_ADMIN. Marks a single session revoked and caches an access-token deny"
              + " marker so the current JWT can no longer access protected APIs.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Session revoked",
        content = @Content(schema = @Schema(implementation = RevokeSessionResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Session not found")
  })
  public ResponseEntity<ApiResponse<RevokeSessionResponse>> revokeSession(
      @PathVariable UUID sessionId,
      @Valid @RequestBody(required = false) SecurityActionRequest request) {
    return ok(
        "/admin/sessions/" + sessionId + "/revoke",
        adminAuditSecurityService.revokeSession(sessionId, request));
  }

  @GetMapping("/admin/devices")
  @Operation(
      summary = "List trusted devices",
      description =
          "Actor: SYSTEM_ADMIN. Audits registered devices and their trust status across tenants."
              + " Read-only; manager staff-device approval uses manager endpoints.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Devices loaded",
        content = @Content(schema = @Schema(implementation = PageResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid filter or pagination value"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required")
  })
  public ResponseEntity<ApiResponse<PageResponse<AdminDeviceResponse>>> getDevices(
      @RequestParam(required = false) UUID tenantId,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return ok(
        "/admin/devices", adminAuditSecurityService.getDevices(tenantId, status, page, size));
  }

  @PostMapping("/admin/devices/{deviceId}/revoke")
  @Operation(
      summary = "Revoke one trusted device",
      description =
          "Actor: SYSTEM_ADMIN. Suspends a device and revokes sessions tied to it so future"
              + " requests from that fingerprint require a new trust decision.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Device revoked",
        content = @Content(schema = @Schema(implementation = RevokeDeviceResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Device not found")
  })
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
