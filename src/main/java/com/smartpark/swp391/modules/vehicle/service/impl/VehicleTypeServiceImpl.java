package com.smartpark.swp391.modules.vehicle.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.modules.vehicle.dto.request.VehicleTypeUpdateRequest;
import com.smartpark.swp391.modules.vehicle.dto.response.VehicleTypeResponse;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import com.smartpark.swp391.modules.vehicle.service.VehicleTypeService;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class VehicleTypeServiceImpl implements VehicleTypeService {

  VehicleTypeRepository vehicleTypeRepository;

  @Override
  @Transactional
  public VehicleTypeResponse updateVehicleType(UUID id, VehicleTypeUpdateRequest request) {
    if (request.name() == null && request.code() == null && request.active() == null) {
      throw new ApiException(ErrorCode.INVALID_INPUT, "Cần truyền ít nhất một trường để cập nhật");
    }

    VehicleType vehicleType = getVehicleType(id);

    if (request.code() != null) {
      String code = request.code().trim();
      if (vehicleTypeRepository.existsByCodeExceptId(code, id)) {
        throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Mã loại phương tiện đã tồn tại");
      }
      vehicleType.setCode(code);
    }

    if (request.name() != null) {
      vehicleType.setName(request.name().trim());
    }

    if (request.active() != null) {
      vehicleType.setActive(request.active());
    }

    VehicleType saved = vehicleTypeRepository.save(vehicleType);
    log.info("Đã cập nhật loại phương tiện id={} code={}", saved.getId(), saved.getCode());
    return toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteVehicleType(UUID id) {
    VehicleType vehicleType = getVehicleType(id);
    vehicleType.setActive(false);
    vehicleType.setDeleted(true);
    vehicleTypeRepository.save(vehicleType);
    log.info("Đã xóa mềm loại phương tiện id={} code={}", id, vehicleType.getCode());
  }

  private VehicleType getVehicleType(UUID id) {
    return vehicleTypeRepository
        .findById(id)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private VehicleTypeResponse toResponse(VehicleType vehicleType) {
    return VehicleTypeResponse.builder()
        .id(vehicleType.getId())
        .name(vehicleType.getName())
        .code(vehicleType.getCode())
        .active(vehicleType.isActive())
        .createdAt(vehicleType.getCreatedAt())
        .build();
  }
}
