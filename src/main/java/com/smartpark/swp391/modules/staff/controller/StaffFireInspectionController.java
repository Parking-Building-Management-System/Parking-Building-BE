package com.smartpark.swp391.modules.staff.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffFireInspectionDueResponse;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffFireInspectionRequest;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffFireInspectionResponse;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffInspectionPhotoPresignRequest;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffInspectionPhotoPresignResponse;
import com.smartpark.swp391.modules.staff.service.StaffFireInspectionService;
import com.smartpark.swp391.modules.staff.support.StaffTenantContext;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/staff/fire-inspections")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('STAFF')")
@SecurityRequirement(name = "bearerAuth")
@Tag(
    name = "Staff Fire Inspections",
    description = "STAFF kiosk APIs for due fire-extinguisher checks and inspection submission")
public class StaffFireInspectionController {

  StaffFireInspectionService staffFireInspectionService;
  StaffTenantContext staffTenantContext;

  @GetMapping("/due")
  @Operation(
      summary = "List due fire inspections",
      description =
          "Actor: STAFF with trusted kiosk context. Lists fire extinguishers in the staff kiosk"
              + " parking that are due or filtered by floor/status. Read-only.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Due inspections loaded"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid floor or status filter"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "STAFF role and trusted kiosk context required")
  })
  public ResponseEntity<ApiResponse<List<StaffFireInspectionDueResponse>>> getDueInspections(
      @RequestParam(required = false) UUID floorId,
      @RequestParam(required = false) FireExtinguisherStatus status,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/fire-inspections/due",
        staffTenantContext.call(
            jwt, () -> staffFireInspectionService.getDueInspections(floorId, status)));
  }

  @PostMapping
  @Operation(
      summary = "Submit fire inspection",
      description =
          "Actor: STAFF with trusted kiosk context. Creates an inspection log, records checklist"
              + " result and optional photo object key, then updates the extinguisher last/next"
              + " inspection timestamps and status.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Inspection submitted"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid checklist, result, or photo object key"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "STAFF role and trusted kiosk context required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Fire extinguisher not found")
  })
  public ResponseEntity<ApiResponse<StaffFireInspectionResponse>> submitInspection(
      @Valid @RequestBody StaffFireInspectionRequest request, @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/fire-inspections",
        staffTenantContext.call(jwt, () -> staffFireInspectionService.submitInspection(request)));
  }

  @PostMapping("/photos/presign-upload")
  @Operation(
      summary = "Presign inspection photo upload",
      description =
          "Actor: STAFF with trusted kiosk context. Creates a tenant-scoped presigned object"
              + " upload URL for one fire-inspection photo. The returned object key is later sent"
              + " in the inspection submission.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Presigned upload created"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid file name or content type"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "STAFF role and trusted kiosk context required")
  })
  public ResponseEntity<ApiResponse<StaffInspectionPhotoPresignResponse>> presignInspectionPhoto(
      @Valid @RequestBody StaffInspectionPhotoPresignRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/fire-inspections/photos/presign-upload",
        staffTenantContext.call(jwt, () -> staffFireInspectionService.createPhotoUpload(request)));
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
