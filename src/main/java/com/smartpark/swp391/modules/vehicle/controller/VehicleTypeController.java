package com.smartpark.swp391.modules.vehicle.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.vehicle.dto.request.VehicleTypeUpdateRequest;
import com.smartpark.swp391.modules.vehicle.dto.response.VehicleTypeResponse;
import com.smartpark.swp391.modules.vehicle.service.VehicleTypeService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vehicle-types")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasAuthority('PERM_QUYEN_TREN_APP.BAI_XE.LOAI_XE.MANAGE')")
@Tag(name = "Vehicle Types", description = "Quản lý master data loại phương tiện")
@SecurityRequirement(name = "bearerAuth")
public class VehicleTypeController {

  VehicleTypeService vehicleTypeService;

  @PatchMapping("/{id}")
  @Operation(summary = "Cập nhật loại phương tiện")
  public ResponseEntity<ApiResponse<VehicleTypeResponse>> updateVehicleType(
      @PathVariable UUID id, @Valid @RequestBody VehicleTypeUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.<VehicleTypeResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Cập nhật loại phương tiện thành công")
            .result(vehicleTypeService.updateVehicleType(id, request))
            .timestamp(Instant.now())
            .path("/vehicle-types/" + id)
            .build());
  }

  @DeleteMapping("/{id}")
  @Operation(summary = "Xóa mềm loại phương tiện")
  public ResponseEntity<ApiResponse<Void>> deleteVehicleType(@PathVariable UUID id) {
    vehicleTypeService.deleteVehicleType(id);
    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Xóa loại phương tiện thành công")
            .timestamp(Instant.now())
            .path("/vehicle-types/" + id)
            .build());
  }
}
