package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.manager.dto.map.FloorMapDetailResponse;
import com.smartpark.swp391.modules.manager.dto.map.FloorMapRequest;
import com.smartpark.swp391.modules.manager.dto.map.FloorMapResponse;
import com.smartpark.swp391.modules.manager.dto.map.SlotCoordinateBulkRequest;
import com.smartpark.swp391.modules.manager.dto.map.SlotCoordinateBulkResponse;
import com.smartpark.swp391.modules.manager.dto.map.SlotCoordinateRequest;
import com.smartpark.swp391.modules.manager.dto.map.SlotCoordinateResponse;
import com.smartpark.swp391.modules.manager.service.ManagerFacilityMapService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/manager")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Manager Facility Map", description = "PARKING_MANAGER floor map and slot coordinates")
public class ManagerFacilityMapController {

  ManagerFacilityMapService managerFacilityMapService;
  ManagerTenantContext managerTenantContext;

  @PatchMapping("/floors/{id}/map")
  @Operation(summary = "Update floor map image")
  public ResponseEntity<ApiResponse<FloorMapResponse>> updateFloorMap(
      @PathVariable UUID id,
      @Valid @RequestBody FloorMapRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/floors/" + id + "/map",
        managerTenantContext.call(
            jwt, () -> managerFacilityMapService.updateFloorMap(id, request)));
  }

  @GetMapping("/floors/{id}/map")
  @Operation(summary = "Get floor map and slot coordinates")
  public ResponseEntity<ApiResponse<FloorMapDetailResponse>> getFloorMap(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/floors/" + id + "/map",
        managerTenantContext.call(jwt, () -> managerFacilityMapService.getFloorMap(id)));
  }

  @PatchMapping("/slots/{id}/coordinate")
  @Operation(summary = "Update one slot coordinate")
  public ResponseEntity<ApiResponse<SlotCoordinateResponse>> updateSlotCoordinate(
      @PathVariable UUID id,
      @Valid @RequestBody SlotCoordinateRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/slots/" + id + "/coordinate",
        managerTenantContext.call(
            jwt, () -> managerFacilityMapService.updateSlotCoordinate(id, request)));
  }

  @PatchMapping("/slots/coordinates")
  @Operation(summary = "Bulk update slot coordinates")
  public ResponseEntity<ApiResponse<SlotCoordinateBulkResponse>> updateSlotCoordinates(
      @Valid @RequestBody SlotCoordinateBulkRequest request, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/slots/coordinates",
        managerTenantContext.call(
            jwt, () -> managerFacilityMapService.updateSlotCoordinates(request)));
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
