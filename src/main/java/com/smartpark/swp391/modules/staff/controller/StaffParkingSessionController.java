package com.smartpark.swp391.modules.staff.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionCheckInRequest;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionCheckInResponse;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionPhotoPresignRequest;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionPhotoPresignResponse;
import com.smartpark.swp391.modules.staff.dto.exit.CompleteExitRequest;
import com.smartpark.swp391.modules.staff.dto.exit.CompleteExitResponse;
import com.smartpark.swp391.modules.staff.dto.exit.ExitPreviewRequest;
import com.smartpark.swp391.modules.staff.dto.exit.ExitPreviewResponse;
import com.smartpark.swp391.modules.staff.service.StaffParkingSessionService;
import com.smartpark.swp391.modules.staff.support.StaffTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/staff/parking-sessions")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('STAFF')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Staff Parking Sessions", description = "STAFF entry gate parking session APIs")
public class StaffParkingSessionController {

  StaffParkingSessionService staffParkingSessionService;
  StaffTenantContext staffTenantContext;

  @PostMapping("/check-in")
  @Operation(
      summary = "Check in a vehicle",
      description =
          "Creates an ACTIVE parking session for the authenticated STAFF tenant, assigns the first"
              + " available slot under the requested parking, and marks the slot occupied.",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              required = true,
              content =
                  @Content(
                      schema = @Schema(implementation = ParkingSessionCheckInRequest.class),
                      examples =
                          @ExampleObject(
                              name = "CheckIn",
                              value =
                                  """
                                  {
                                    "plateNumber": "51A-12345",
                                    "cardCode": "VIN-RFID-001",
                                    "parkingId": "8fe2f5ec-2f7b-3760-9f46-c4fc5c1f5d5e",
                                    "vehicleTypeId": "a6b4cdbd-74c8-45bc-9ac2-b533423a7892",
                                    "entryImageUrl": "tenants/.../parking-sessions/entry-verification/entry-overview/51A-12345.jpg",
                                    "licensePlateImageUrl": "tenants/.../parking-sessions/entry-verification/license-plate/51A-12345.jpg"
                                  }
                                  """))))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "Parking session created successfully",
      content = @Content(schema = @Schema(implementation = ParkingSessionCheckInResponse.class)))
  public ResponseEntity<ApiResponse<ParkingSessionCheckInResponse>> checkIn(
      @Valid @RequestBody ParkingSessionCheckInRequest request, @AuthenticationPrincipal Jwt jwt) {
    ParkingSessionCheckInResponse result =
        staffTenantContext.call(jwt, () -> staffParkingSessionService.checkIn(request));

    return ResponseEntity.ok(
        ApiResponse.<ParkingSessionCheckInResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(result)
            .timestamp(Instant.now())
            .path("/staff/parking-sessions/check-in")
            .build());
  }

  @PostMapping("/photos/presign-upload")
  @Operation(summary = "Create presigned upload URL for staff entry verification photos")
  public ResponseEntity<ApiResponse<ParkingSessionPhotoPresignResponse>> presignEntryPhoto(
      @Valid @RequestBody ParkingSessionPhotoPresignRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    ParkingSessionPhotoPresignResponse result =
        staffTenantContext.call(jwt, () -> staffParkingSessionService.createPhotoUpload(request));

    return ResponseEntity.ok(
        ApiResponse.<ParkingSessionPhotoPresignResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(result)
            .timestamp(Instant.now())
            .path("/staff/parking-sessions/photos/presign-upload")
            .build());
  }

  @PostMapping("/exit-preview")
  @Operation(summary = "Preview exit decision for an active parking session")
  public ResponseEntity<ApiResponse<ExitPreviewResponse>> previewExit(
      @Valid @RequestBody ExitPreviewRequest request, @AuthenticationPrincipal Jwt jwt) {
    ExitPreviewResponse result =
        staffTenantContext.call(jwt, () -> staffParkingSessionService.previewExit(request));

    return ResponseEntity.ok(
        ApiResponse.<ExitPreviewResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(result)
            .timestamp(Instant.now())
            .path("/staff/parking-sessions/exit-preview")
            .build());
  }

  @PostMapping("/complete-exit")
  @Operation(summary = "Complete exit and release the assigned slot")
  public ResponseEntity<ApiResponse<CompleteExitResponse>> completeExit(
      @Valid @RequestBody CompleteExitRequest request, @AuthenticationPrincipal Jwt jwt) {
    CompleteExitResponse result =
        staffTenantContext.call(jwt, () -> staffParkingSessionService.completeExit(request));

    return ResponseEntity.ok(
        ApiResponse.<CompleteExitResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(result)
            .timestamp(Instant.now())
            .path("/staff/parking-sessions/complete-exit")
            .build());
  }
}
