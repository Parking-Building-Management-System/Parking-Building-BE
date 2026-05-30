package com.smartpark.swp391.modules.vehicle.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.persistence.UuidV7;
import com.smartpark.swp391.modules.vehicle.dto.VehicleTypeCreateRequest;
import com.smartpark.swp391.modules.vehicle.dto.VehicleTypeDTO;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import com.smartpark.swp391.modules.vehicle.service.VehicleTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleTypeServiceImpl implements VehicleTypeService {

  private final VehicleTypeRepository vehicleTypeRepository;

  @Override
  @Transactional
  public VehicleTypeDTO createVehicleType(VehicleTypeCreateRequest request) {
    log.info("Bắt đầu tạo loại phương tiện: {}", request.name());

    if (vehicleTypeRepository.existsByNameIgnoreCaseAndIsDeletedFalse(request.name())) {
      log.warn("Tạo loại phương tiện thất bại - Tên đã tồn tại: {}", request.name());
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Tên loại phương tiện đã tồn tại");
    }

    VehicleType vehicleType =
        VehicleType.builder()
            .id(UuidV7.random().toString())
            .name(request.name().trim())
            .description(StringUtils.hasText(request.description()) ? request.description().trim() : null)
            .isDeleted(false)
            .build();

    vehicleType = vehicleTypeRepository.save(vehicleType);
    log.info("Tạo loại phương tiện thành công - ID: {}", vehicleType.getId());
    return toDto(vehicleType);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<VehicleTypeDTO> getAllVehicleTypes(Pageable pageable, String searchKey) {
    String normalizedSearch = StringUtils.hasText(searchKey) ? searchKey.trim() : null;
    return vehicleTypeRepository
        .findAllActive(normalizedSearch, pageable)
        .map(this::toDto);
  }

  private VehicleTypeDTO toDto(VehicleType entity) {
    return new VehicleTypeDTO(
        entity.getId(),
        entity.getName(),
        entity.getDescription(),
        entity.getCreatedDate(),
        entity.getUpdatedDate());
  }
}
