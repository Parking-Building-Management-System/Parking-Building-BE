package com.smartpark.swp391.modules.identity.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.security.annotation.RateLimit;
import com.smartpark.swp391.modules.identity.dto.authentication.request.StaffPasswordResetCreateRequest;
import com.smartpark.swp391.modules.identity.service.auth.StaffPasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/password-reset-requests")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(name = "Staff Password Reset Requests")
public class StaffPasswordResetRequestController {

  static final String ACCEPTED_MESSAGE =
      "If the account is eligible, the password reset request has been sent to the parking"
          + " manager.";

  StaffPasswordResetService staffPasswordResetService;

  @PostMapping
  @Operation(summary = "Request a manager-reviewed Staff password reset")
  @SecurityRequirements
  @RateLimit(
      limit = 5,
      duration = 60,
      type = RateLimit.Type.REQUEST_FIELD,
      fieldName = "'password-reset:' + #request.normalizedEmail()")
  public ResponseEntity<ApiResponse<Void>> createRequest(
      @Valid @RequestBody StaffPasswordResetCreateRequest request) {
    staffPasswordResetService.requestReset(request.normalizedEmail());
    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ACCEPTED_MESSAGE)
            .timestamp(Instant.now())
            .path("/auth/password-reset-requests")
            .build());
  }
}
