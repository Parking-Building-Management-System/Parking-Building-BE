package com.smartpark.swp391.modules.staff.service.impl;

import com.smartpark.swp391.modules.admin.dto.masterdata.AdminVehicleTypeResponse;
import com.smartpark.swp391.modules.staff.service.StaffMasterDataService;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import java.util.List;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class StaffMasterDataServiceImpl implements StaffMasterDataService {

  VehicleTypeRepository vehicleTypeRepository;

  @Override
  @Transactional(readOnly = true)
  public List<AdminVehicleTypeResponse> getActiveVehicleTypes() {
    return vehicleTypeRepository.findAllByActiveTrueAndDeletedFalseOrderByNameAsc().stream()
        .map(this::toResponse)
        .toList();
  }

  private AdminVehicleTypeResponse toResponse(VehicleType vehicleType) {
    return AdminVehicleTypeResponse.builder()
        .id(vehicleType.getId())
        .name(vehicleType.getName())
        .code(vehicleType.getCode())
        .active(vehicleType.isActive())
        .createdAt(vehicleType.getCreatedAt())
        .build();
  }
}
