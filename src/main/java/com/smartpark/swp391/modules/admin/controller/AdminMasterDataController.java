package com.smartpark.swp391.modules.admin.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.admin.dto.masterdata.AdminRoleResponse;
import com.smartpark.swp391.modules.admin.dto.masterdata.AdminVehicleTypeRequest;
import com.smartpark.swp391.modules.admin.dto.masterdata.AdminVehicleTypeResponse;
import com.smartpark.swp391.modules.admin.service.AdminMasterDataService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/master-data")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
// @PreAuthorize("hasRole('SYSTEM_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Master Data", description = "SYSTEM_ADMIN global configuration APIs")
public class AdminMasterDataController {

    AdminMasterDataService adminMasterDataService;

    @GetMapping("/vehicle-types")
    @Operation(
            summary = "List vehicle types",
            description =
                    "Returns all global vehicle categories. The list is cached in Redis without TTL"
                            + " and evicted only when vehicle type data changes.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Vehicle types loaded successfully",
            content =
                    @Content(
                            array =
                                    @ArraySchema(
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    AdminVehicleTypeResponse
                                                                            .class))))
    public ResponseEntity<ApiResponse<List<AdminVehicleTypeResponse>>> getVehicleTypes() {
        return ResponseEntity.ok(
                ApiResponse.<List<AdminVehicleTypeResponse>>builder()
                        .code(ErrorCode.SUCCESS.getCode())
                        .message(ErrorCode.SUCCESS.getDefaultMessage())
                        .result(adminMasterDataService.getVehicleTypes())
                        .timestamp(Instant.now())
                        .path("/admin/master-data/vehicle-types")
                        .build());
    }

    @PostMapping("/vehicle-types")
    @Operation(summary = "Create vehicle type", description = "Creates a global vehicle category.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Vehicle type created successfully",
            content = @Content(schema = @Schema(implementation = AdminVehicleTypeResponse.class)))
    public ResponseEntity<ApiResponse<AdminVehicleTypeResponse>> createVehicleType(
            @Valid @RequestBody AdminVehicleTypeRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<AdminVehicleTypeResponse>builder()
                        .code(ErrorCode.SUCCESS.getCode())
                        .message("Tạo loại phương tiện thành công")
                        .result(adminMasterDataService.createVehicleType(request))
                        .timestamp(Instant.now())
                        .path("/admin/master-data/vehicle-types")
                        .build());
    }

    @PutMapping("/vehicle-types/{id}")
    @Operation(summary = "Update vehicle type", description = "Updates a global vehicle category.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Vehicle type updated successfully",
            content = @Content(schema = @Schema(implementation = AdminVehicleTypeResponse.class)))
    public ResponseEntity<ApiResponse<AdminVehicleTypeResponse>> updateVehicleType(
            @PathVariable UUID id, @Valid @RequestBody AdminVehicleTypeRequest request) {
        return ResponseEntity.ok(
                ApiResponse.<AdminVehicleTypeResponse>builder()
                        .code(ErrorCode.SUCCESS.getCode())
                        .message("Cập nhật loại phương tiện thành công")
                        .result(adminMasterDataService.updateVehicleType(id, request))
                        .timestamp(Instant.now())
                        .path("/admin/master-data/vehicle-types/" + id)
                        .build());
    }

    @DeleteMapping("/vehicle-types/{id}")
    @Operation(
            summary = "Delete vehicle type",
            description = "Soft-deletes a global vehicle category.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Vehicle type deleted successfully",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
    public ResponseEntity<ApiResponse<Void>> deleteVehicleType(@PathVariable UUID id) {
        adminMasterDataService.deleteVehicleType(id);
        return ResponseEntity.ok(
                ApiResponse.<Void>builder()
                        .code(ErrorCode.SUCCESS.getCode())
                        .message("Xóa loại phương tiện thành công")
                        .timestamp(Instant.now())
                        .path("/admin/master-data/vehicle-types/" + id)
                        .build());
    }

    @GetMapping("/roles")
    @Operation(
            summary = "List roles",
            description = "Returns all configured global roles from the database.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Roles loaded successfully",
            content =
                    @Content(
                            array =
                                    @ArraySchema(
                                            schema =
                                                    @Schema(
                                                            implementation =
                                                                    AdminRoleResponse.class))))
    public ResponseEntity<ApiResponse<List<AdminRoleResponse>>> getRoles() {
        return ResponseEntity.ok(
                ApiResponse.<List<AdminRoleResponse>>builder()
                        .code(ErrorCode.SUCCESS.getCode())
                        .message(ErrorCode.SUCCESS.getDefaultMessage())
                        .result(adminMasterDataService.getRoles())
                        .timestamp(Instant.now())
                        .path("/admin/master-data/roles")
                        .build());
    }
}
