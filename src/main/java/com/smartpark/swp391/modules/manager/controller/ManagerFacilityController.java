package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.manager.dto.facility.FloorRequest;
import com.smartpark.swp391.modules.manager.dto.facility.FloorResponse;
import com.smartpark.swp391.modules.manager.dto.facility.ParkingResponse;
import com.smartpark.swp391.modules.manager.dto.facility.ParkingStatusResponse;
import com.smartpark.swp391.modules.manager.dto.facility.ZoneRequest;
import com.smartpark.swp391.modules.manager.dto.facility.ZoneResponse;
import com.smartpark.swp391.modules.manager.dto.topology.ParkingTopologyResponse;
import com.smartpark.swp391.modules.manager.service.ManagerFacilityService;
import com.smartpark.swp391.modules.manager.support.ManagerTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@Tag(name = "Manager Facility", description = "PARKING_MANAGER facility structure APIs")
public class ManagerFacilityController {

  ManagerFacilityService managerFacilityService;
  ManagerTenantContext managerTenantContext;

  @GetMapping("/parkings")
  @Operation(
      summary = "List parkings",
      description = "Lists tenant parkings with live slot capacity.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "Parkings loaded successfully",
      content =
          @Content(array = @ArraySchema(schema = @Schema(implementation = ParkingResponse.class))))
  public ResponseEntity<ApiResponse<List<ParkingResponse>>> getParkings(
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/parkings", managerTenantContext.call(jwt, managerFacilityService::getParkings));
  }

  @PatchMapping("/parkings/{id}/status")
  @Operation(
      summary = "Toggle parking status",
      description = "Toggles a tenant parking between ACTIVE and MAINTENANCE.")
  public ResponseEntity<ApiResponse<ParkingStatusResponse>> toggleParkingStatus(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/parkings/" + id + "/status",
        managerTenantContext.call(jwt, () -> managerFacilityService.toggleParkingStatus(id)));
  }

  @GetMapping("/parkings/{id}/topology")
  @Operation(
      summary = "Get parking topology",
      description =
          "Returns one parking with nested floors and zones for frontend tree rendering. The payload"
              + " is cached per tenant and parking.")
  public ResponseEntity<ApiResponse<ParkingTopologyResponse>> getTopology(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/parkings/" + id + "/topology",
        managerTenantContext.call(jwt, () -> managerFacilityService.getTopology(id)));
  }

  @GetMapping("/parkings/{parkingId}/floors")
  @Operation(summary = "List floors", description = "Lists floors under a tenant parking.")
  public ResponseEntity<ApiResponse<List<FloorResponse>>> getFloors(
      @PathVariable UUID parkingId, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/parkings/" + parkingId + "/floors",
        managerTenantContext.call(jwt, () -> managerFacilityService.getFloors(parkingId)));
  }

  @PostMapping("/parkings/{parkingId}/floors")
  @Operation(summary = "Create floor", description = "Creates a floor under a tenant parking.")
  public ResponseEntity<ApiResponse<FloorResponse>> createFloor(
      @PathVariable UUID parkingId,
      @Valid @RequestBody FloorRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/parkings/" + parkingId + "/floors",
        managerTenantContext.call(
            jwt, () -> managerFacilityService.createFloor(parkingId, request)));
  }

  @GetMapping("/floors/{id}")
  @Operation(summary = "Get floor", description = "Gets one tenant floor by id.")
  public ResponseEntity<ApiResponse<FloorResponse>> getFloor(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/floors/" + id,
        managerTenantContext.call(jwt, () -> managerFacilityService.getFloor(id)));
  }

  @PutMapping("/floors/{id}")
  @Operation(summary = "Update floor", description = "Updates one tenant floor by id.")
  public ResponseEntity<ApiResponse<FloorResponse>> updateFloor(
      @PathVariable UUID id,
      @Valid @RequestBody FloorRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/floors/" + id,
        managerTenantContext.call(jwt, () -> managerFacilityService.updateFloor(id, request)));
  }

  @DeleteMapping("/floors/{id}")
  @Operation(summary = "Delete floor", description = "Soft-deletes an empty tenant floor.")
  public ResponseEntity<ApiResponse<Void>> deleteFloor(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    managerTenantContext.run(jwt, () -> managerFacilityService.deleteFloor(id));
    return ok("/manager/floors/" + id, null);
  }

  @GetMapping("/floors/{floorId}/zones")
  @Operation(summary = "List zones", description = "Lists zones under a tenant floor.")
  public ResponseEntity<ApiResponse<List<ZoneResponse>>> getZones(
      @PathVariable UUID floorId, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/floors/" + floorId + "/zones",
        managerTenantContext.call(jwt, () -> managerFacilityService.getZones(floorId)));
  }

  @PostMapping("/floors/{floorId}/zones")
  @Operation(
      summary = "Create zone",
      description = "Creates a zone and validates vehicleTypeCode against global vehicle types.")
  public ResponseEntity<ApiResponse<ZoneResponse>> createZone(
      @PathVariable UUID floorId,
      @Valid @RequestBody ZoneRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/floors/" + floorId + "/zones",
        managerTenantContext.call(jwt, () -> managerFacilityService.createZone(floorId, request)));
  }

  @GetMapping("/zones/{id}")
  @Operation(summary = "Get zone", description = "Gets one tenant zone by id.")
  public ResponseEntity<ApiResponse<ZoneResponse>> getZone(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/zones/" + id,
        managerTenantContext.call(jwt, () -> managerFacilityService.getZone(id)));
  }

  @PutMapping("/zones/{id}")
  @Operation(
      summary = "Update zone",
      description = "Updates one tenant zone and revalidates vehicleTypeCode.")
  public ResponseEntity<ApiResponse<ZoneResponse>> updateZone(
      @PathVariable UUID id,
      @Valid @RequestBody ZoneRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/manager/zones/" + id,
        managerTenantContext.call(jwt, () -> managerFacilityService.updateZone(id, request)));
  }

  @DeleteMapping("/zones/{id}")
  @Operation(summary = "Delete zone", description = "Soft-deletes an empty tenant zone.")
  public ResponseEntity<ApiResponse<Void>> deleteZone(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    managerTenantContext.run(jwt, () -> managerFacilityService.deleteZone(id));
    return ok("/manager/zones/" + id, null);
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
