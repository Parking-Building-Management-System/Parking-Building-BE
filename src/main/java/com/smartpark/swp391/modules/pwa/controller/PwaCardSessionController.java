package com.smartpark.swp391.modules.pwa.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.pwa.dto.CardActiveSessionResponse;
import com.smartpark.swp391.modules.pwa.service.PwaCardSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/pwa/cards")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(name = "PWA Card Session", description = "Public card QR active session guide APIs")
public class PwaCardSessionController {

  PwaCardSessionService pwaCardSessionService;

  @GetMapping("/{qrToken}/active-session")
  @Operation(summary = "Resolve active parking session by RFID card QR token")
  public ResponseEntity<ApiResponse<CardActiveSessionResponse>> getActiveSession(
      @PathVariable String qrToken) {
    return ResponseEntity.ok(
        ApiResponse.<CardActiveSessionResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(pwaCardSessionService.getActiveSession(qrToken))
            .timestamp(Instant.now())
            .path("/pwa/cards/" + qrToken + "/active-session")
            .build());
  }
}
