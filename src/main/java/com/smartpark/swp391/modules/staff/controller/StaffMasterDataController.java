package com.smartpark.swp391.modules.staff.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.admin.dto.masterdata.AdminVehicleTypeResponse;
import com.smartpark.swp391.modules.staff.service.StaffMasterDataService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/staff/master-data")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('STAFF')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Staff Master Data", description = "STAFF read-only lookup APIs")
public class StaffMasterDataController {

  StaffMasterDataService staffMasterDataService;

  @GetMapping("/vehicle-types")
  @Operation(summary = "List active vehicle types for staff check-in")
  public ResponseEntity<ApiResponse<List<AdminVehicleTypeResponse>>> getVehicleTypes() {
    return ResponseEntity.ok(
        ApiResponse.<List<AdminVehicleTypeResponse>>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(staffMasterDataService.getActiveVehicleTypes())
            .timestamp(Instant.now())
            .path("/staff/master-data/vehicle-types")
            .build());
  }
}
