package com.smartpark.swp391.modules.vehicle.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.vehicle.dto.VehicleTypeCreateRequest;
import com.smartpark.swp391.modules.vehicle.dto.VehicleTypeDTO;
import com.smartpark.swp391.modules.vehicle.service.VehicleTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vehicle-types")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(name = "Vehicle Type Management", description = "Quản lý loại phương tiện")
public class VehicleTypeController {

  VehicleTypeService vehicleTypeService;

  @Operation(
      summary = "Tạo mới loại phương tiện",
      description = "Tạo một loại phương tiện mới trong hệ thống bãi xe.")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "Tạo loại phương tiện thành công",
            content = @Content(schema = @Schema(implementation = VehicleTypeDTO.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Dữ liệu không hợp lệ",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Tên loại phương tiện đã tồn tại",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @PostMapping
  public ResponseEntity<ApiResponse<VehicleTypeDTO>> create(
      @Valid @RequestBody VehicleTypeCreateRequest request) {
    VehicleTypeDTO created = vehicleTypeService.createVehicleType(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            ApiResponse.<VehicleTypeDTO>builder()
                .code(ErrorCode.SUCCESS.getCode())
                .message("Tạo loại phương tiện thành công")
                .result(created)
                .timestamp(Instant.now())
                .build());
  }

  @Operation(
      summary = "Lấy danh sách loại phương tiện",
      description =
          "Lấy danh sách có phân trang, lọc theo tên (search), sắp xếp mặc định theo ngày tạo"
              + " giảm dần.")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Lấy danh sách thành công",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<VehicleTypeDTO>>> getAll(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String search) {
    Pageable pageable =
        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
    Page<VehicleTypeDTO> resultPage = vehicleTypeService.getAllVehicleTypes(pageable, search);

    PageResponse<VehicleTypeDTO> pageResponse =
        new PageResponse<>(
            resultPage.getContent(),
            resultPage.getNumber(),
            resultPage.getSize(),
            resultPage.getTotalElements(),
            resultPage.getTotalPages());

    return ResponseEntity.ok(
        ApiResponse.<PageResponse<VehicleTypeDTO>>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Lấy danh sách loại phương tiện thành công")
            .result(pageResponse)
            .timestamp(Instant.now())
            .build());
  }
}
