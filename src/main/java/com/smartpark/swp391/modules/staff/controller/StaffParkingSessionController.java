package com.smartpark.swp391.modules.staff.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionCheckInRequest;
import com.smartpark.swp391.modules.staff.dto.ParkingSessionCheckInResponse;
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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(
    name = "Staff Parking Sessions",
    description = "STAFF kiosk APIs for vehicle check-in, exit preview, and exit completion")
public class StaffParkingSessionController {

  StaffParkingSessionService staffParkingSessionService;
  StaffTenantContext staffTenantContext;

  @PostMapping("/check-in")
  @Operation(
      summary = "Check in a vehicle",
      description =
          "Actor: STAFF at an ENTRY or ENTRY_EXIT kiosk. Creates an ACTIVE parking session for"
              + " the authenticated tenant, validates RFID card availability, assigns the first"
              + " available slot under the requested parking, and marks that slot OCCUPIED.",
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
                                    "entryImageUrl": "https://example.com/entry/51A-12345.jpg"
                                  }
                                  """))))
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "Parking session created successfully",
      content = @Content(schema = @Schema(implementation = ParkingSessionCheckInResponse.class)))
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid plate/card/vehicle type or card already in use"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "STAFF role, trusted device, or kiosk context required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Parking, RFID card, vehicle type, or available slot not found")
  })
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

  @PostMapping("/exit-preview")
  @Operation(
      summary = "Preview exit decision",
      description =
          "Actor: STAFF at an EXIT or ENTRY_EXIT kiosk. Resolves the active session from the"
              + " scanned RFID card, validates the kiosk parking context, calculates current fee,"
              + " and returns whether the gate may open or cash/surcharge is required. Read-only.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Exit preview calculated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid card, inactive session, or pricing rule missing"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "STAFF role and EXIT kiosk context required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Card or active session not found")
  })
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
  @Operation(
      summary = "Complete exit",
      description =
          "Actor: STAFF at an EXIT or ENTRY_EXIT kiosk. Validates the exit decision/payment mode,"
              + " completes the active session, records payment fields when needed, releases the"
              + " assigned slot to AVAILABLE, and keeps the RFID card active for reuse.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Exit completed and slot released"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid payment mode, insufficient cash, duplicate completion, or grace expired"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "STAFF role and EXIT kiosk context required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Parking session not found")
  })
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
