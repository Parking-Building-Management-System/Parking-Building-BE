package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherType;
import com.smartpark.swp391.modules.firesafety.enumType.FireInspectionResult;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherCoordinateRequest;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherRequest;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherResponse;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherStatusRequest;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherSummaryResponse;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireInspectionLogResponse;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireSafetyMapResponse;
import com.smartpark.swp391.modules.manager.service.ManagerFireExtinguisherService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
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

@Validated
@RestController
@RequestMapping("/manager")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Manager Fire Safety", description = "PARKING_MANAGER fire extinguisher APIs")
public class ManagerFireExtinguisherController {

  ManagerFireExtinguisherService managerFireExtinguisherService;
  ManagerTenantContext managerTenantContext;

  @GetMapping("/fire-extinguishers")
  @Operation(summary = "List fire extinguishers")
  public ResponseEntity<ApiResponse<PageResponse<FireExtinguisherResponse>>> getExtinguishers(
      @RequestParam(required = false) UUID parkingId,
      @RequestParam(required = false) UUID floorId,
      @RequestParam(required = false) UUID zoneId,
      @RequestParam(required = false) FireExtinguisherStatus status,
      @RequestParam(required = false) FireExtinguisherType type,
      @RequestParam(required = false) String search,
      @RequestParam(required = false) @Min(0) Integer expiringWithinDays,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/fire-extinguishers",
        managerTenantContext.call(
            jwt,
            () ->
                managerFireExtinguisherService.getExtinguishers(
                    parkingId,
                    floorId,
                    zoneId,
                    status,
                    type,
                    search,
                    expiringWithinDays,
                    page,
                    size)));
  }

  @PostMapping("/fire-extinguishers")
  @Operation(summary = "Create fire extinguisher")
  public ResponseEntity<ApiResponse<FireExtinguisherResponse>> createExtinguisher(
      @Valid @RequestBody FireExtinguisherRequest request, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/fire-extinguishers",
        managerTenantContext.call(
            jwt, () -> managerFireExtinguisherService.createExtinguisher(request)));
  }

  @GetMapping("/fire-extinguishers/{id}")
  @Operation(summary = "Get fire extinguisher")
  public ResponseEntity<ApiResponse<FireExtinguisherResponse>> getExtinguisher(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/fire-extinguishers/" + id,
        managerTenantContext.call(jwt, () -> managerFireExtinguisherService.getExtinguisher(id)));
  }

  @PutMapping("/fire-extinguishers/{id}")
  @Operation(summary = "Update fire extinguisher")
  public ResponseEntity<ApiResponse<FireExtinguisherResponse>> updateExtinguisher(
      @PathVariable UUID id,
      @Valid @RequestBody FireExtinguisherRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/fire-extinguishers/" + id,
        managerTenantContext.call(
            jwt, () -> managerFireExtinguisherService.updateExtinguisher(id, request)));
  }

  @PatchMapping("/fire-extinguishers/{id}/status")
  @Operation(summary = "Update fire extinguisher status")
  public ResponseEntity<ApiResponse<FireExtinguisherResponse>> updateStatus(
      @PathVariable UUID id,
      @Valid @RequestBody FireExtinguisherStatusRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/fire-extinguishers/" + id + "/status",
        managerTenantContext.call(
            jwt, () -> managerFireExtinguisherService.updateStatus(id, request)));
  }

  @PatchMapping("/fire-extinguishers/{id}/coordinate")
  @Operation(summary = "Update fire extinguisher map coordinate")
  public ResponseEntity<ApiResponse<FireExtinguisherResponse>> updateCoordinate(
      @PathVariable UUID id,
      @Valid @RequestBody FireExtinguisherCoordinateRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/fire-extinguishers/" + id + "/coordinate",
        managerTenantContext.call(
            jwt, () -> managerFireExtinguisherService.updateCoordinate(id, request)));
  }

  @DeleteMapping("/fire-extinguishers/{id}")
  @Operation(summary = "Delete fire extinguisher")
  public ResponseEntity<ApiResponse<Void>> deleteExtinguisher(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    managerTenantContext.run(jwt, () -> managerFireExtinguisherService.deleteExtinguisher(id));
    return ok("/manager/fire-extinguishers/" + id, null);
  }

  @GetMapping("/fire-extinguishers/summary")
  @Operation(summary = "Get fire extinguisher summary")
  public ResponseEntity<ApiResponse<FireExtinguisherSummaryResponse>> getSummary(
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/fire-extinguishers/summary",
        managerTenantContext.call(jwt, managerFireExtinguisherService::getSummary));
  }

  @GetMapping("/floors/{floorId}/fire-safety-map")
  @Operation(summary = "Get floor fire safety map pins")
  public ResponseEntity<ApiResponse<FireSafetyMapResponse>> getFireSafetyMap(
      @PathVariable UUID floorId, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/floors/" + floorId + "/fire-safety-map",
        managerTenantContext.call(
            jwt, () -> managerFireExtinguisherService.getFireSafetyMap(floorId)));
  }

  @GetMapping("/fire-inspections/logs")
  @Operation(summary = "List fire extinguisher inspection logs")
  public ResponseEntity<ApiResponse<PageResponse<FireInspectionLogResponse>>> getInspectionLogs(
      @RequestParam(required = false) UUID extinguisherId,
      @RequestParam(required = false) UUID parkingId,
      @RequestParam(required = false) UUID floorId,
      @RequestParam(required = false) FireInspectionResult result,
      @RequestParam(required = false) LocalDateTime from,
      @RequestParam(required = false) LocalDateTime to,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/fire-inspections/logs",
        managerTenantContext.call(
            jwt,
            () ->
                managerFireExtinguisherService.getInspectionLogs(
                    extinguisherId, parkingId, floorId, result, from, to, page, size)));
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
