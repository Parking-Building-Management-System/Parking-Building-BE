package com.smartpark.swp391.modules.staff.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.staff.dto.AvailableRfidCardResponse;
import com.smartpark.swp391.modules.staff.service.StaffRfidCardService;
import com.smartpark.swp391.modules.staff.support.StaffTenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/staff/rfid-cards")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('STAFF')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Staff RFID Cards", description = "STAFF kiosk APIs for available card lookup")
public class StaffRfidCardController {

  StaffRfidCardService staffRfidCardService;
  StaffTenantContext staffTenantContext;

  @GetMapping("/available")
  @Operation(
      summary = "List available RFID cards",
      description =
          "Actor: STAFF with trusted kiosk context. Lists ACTIVE RFID cards in the tenant that"
              + " are not linked to an ACTIVE parking session. Used before check-in.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Available RFID cards loaded"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid limit or search query"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "STAFF role and trusted kiosk context required")
  })
  public ResponseEntity<ApiResponse<List<AvailableRfidCardResponse>>> getAvailableCards(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) @Min(1) @Max(100) Integer limit,
      @AuthenticationPrincipal Jwt jwt) {
    return ok(
        "/staff/rfid-cards/available",
        staffTenantContext.call(jwt, () -> staffRfidCardService.getAvailableCards(search, limit)));
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
