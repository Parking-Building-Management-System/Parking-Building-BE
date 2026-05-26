package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskRequest;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskResponse;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskStaffResponse;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskStatusRequest;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskUpdateRequest;
import com.smartpark.swp391.modules.manager.service.ManagerKioskService;
import com.smartpark.swp391.modules.manager.support.ManagerTenantContext;
import com.smartpark.swp391.modules.operation.enumType.KioskStatus;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/kiosks")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tags({
  @Tag(name = "Manager Kiosks", description = "PARKING_MANAGER kiosk and gate APIs"),
  @Tag(name = "Manager Kiosk Staff", description = "PARKING_MANAGER kiosk staff assignments")
})
public class ManagerKioskController {

  ManagerKioskService managerKioskService;
  ManagerTenantContext managerTenantContext;

  @GetMapping
  @Operation(summary = "List kiosks in current tenant")
  public ResponseEntity<ApiResponse<List<ManagerKioskResponse>>> getKiosks(
      @RequestParam(required = false) UUID parkingId,
      @RequestParam(required = false) KioskStatus status,
      @RequestParam(required = false) KioskType type,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/kiosks",
        managerTenantContext.call(
            jwt, () -> managerKioskService.getKiosks(parkingId, status, type)));
  }

  @PostMapping
  @Operation(summary = "Create kiosk under a tenant parking")
  public ResponseEntity<ApiResponse<ManagerKioskResponse>> createKiosk(
      @Valid @RequestBody ManagerKioskRequest request, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/kiosks",
        managerTenantContext.call(jwt, () -> managerKioskService.createKiosk(request)));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get kiosk")
  public ResponseEntity<ApiResponse<ManagerKioskResponse>> getKiosk(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/kiosks/" + id,
        managerTenantContext.call(jwt, () -> managerKioskService.getKiosk(id)));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update kiosk")
  public ResponseEntity<ApiResponse<ManagerKioskResponse>> updateKiosk(
      @PathVariable UUID id,
      @Valid @RequestBody ManagerKioskUpdateRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/kiosks/" + id,
        managerTenantContext.call(jwt, () -> managerKioskService.updateKiosk(id, request)));
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Update kiosk status")
  public ResponseEntity<ApiResponse<ManagerKioskResponse>> updateStatus(
      @PathVariable UUID id,
      @Valid @RequestBody ManagerKioskStatusRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/kiosks/" + id + "/status",
        managerTenantContext.call(jwt, () -> managerKioskService.updateStatus(id, request)));
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Soft-delete kiosk")
  public ResponseEntity<ApiResponse<Void>> deleteKiosk(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    managerTenantContext.run(jwt, () -> managerKioskService.deleteKiosk(id));
    return ok("/manager/kiosks/" + id, null);
  }

  @GetMapping("/{id}/staff")
  @Operation(summary = "List staff assigned to kiosk")
  @Tag(name = "Manager Kiosk Staff")
  public ResponseEntity<ApiResponse<List<ManagerKioskStaffResponse>>> getStaff(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/kiosks/" + id + "/staff",
        managerTenantContext.call(jwt, () -> managerKioskService.getStaff(id)));
  }

  @PostMapping("/{id}/staff/{staffId}")
  @Operation(summary = "Assign staff to kiosk")
  @Tag(name = "Manager Kiosk Staff")
  public ResponseEntity<ApiResponse<ManagerKioskStaffResponse>> assignStaff(
      @PathVariable UUID id, @PathVariable UUID staffId, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/kiosks/" + id + "/staff/" + staffId,
        managerTenantContext.call(jwt, () -> managerKioskService.assignStaff(id, staffId)));
  }

  @DeleteMapping("/{id}/staff/{staffId}")
  @Operation(summary = "Unassign staff from kiosk")
  @Tag(name = "Manager Kiosk Staff")
  public ResponseEntity<ApiResponse<Void>> unassignStaff(
      @PathVariable UUID id, @PathVariable UUID staffId, @AuthenticationPrincipal Jwt jwt) {
    managerTenantContext.run(jwt, () -> managerKioskService.unassignStaff(id, staffId));
    return ok("/manager/kiosks/" + id + "/staff/" + staffId, null);
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
