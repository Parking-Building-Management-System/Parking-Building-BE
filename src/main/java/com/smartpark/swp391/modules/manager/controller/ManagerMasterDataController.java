package com.smartpark.swp391.modules.manager.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.admin.dto.masterdata.AdminVehicleTypeResponse;
import com.smartpark.swp391.modules.manager.service.ManagerMasterDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/manager/master-data")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('PARKING_MANAGER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Manager Master Data", description = "PARKING_MANAGER read-only lookup APIs")
public class ManagerMasterDataController {

  ManagerMasterDataService managerMasterDataService;

  @GetMapping("/vehicle-types")
  @Operation(
      summary = "List active vehicle types for manager setup forms",
      description = "Read-only lookup used by zone, pricing, and facility setup forms.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "Vehicle types loaded successfully",
      content =
          @Content(
              array =
                  @ArraySchema(schema = @Schema(implementation = AdminVehicleTypeResponse.class))))
  public ResponseEntity<ApiResponse<List<AdminVehicleTypeResponse>>> getVehicleTypes() {
    return ResponseEntity.ok(
        ApiResponse.<List<AdminVehicleTypeResponse>>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(managerMasterDataService.getActiveVehicleTypes())
            .timestamp(Instant.now())
            .path("/manager/master-data/vehicle-types")
            .build());
  }
}
