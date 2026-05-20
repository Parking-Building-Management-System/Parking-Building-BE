package com.smartpark.swp391.modules.admin.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.cached.redis.service.AdminPortalCacheService;
import com.smartpark.swp391.modules.admin.dto.masterdata.AdminRoleResponse;
import com.smartpark.swp391.modules.admin.dto.masterdata.AdminVehicleTypeRequest;
import com.smartpark.swp391.modules.admin.dto.masterdata.AdminVehicleTypeResponse;
import com.smartpark.swp391.modules.admin.service.AdminMasterDataService;
import com.smartpark.swp391.modules.identity.entity.Role;
import com.smartpark.swp391.modules.identity.repository.RoleRepository;
import com.smartpark.swp391.modules.vehicle.entity.VehicleType;
import com.smartpark.swp391.modules.vehicle.repository.VehicleTypeRepository;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AdminMasterDataServiceImpl implements AdminMasterDataService {

  VehicleTypeRepository vehicleTypeRepository;
  RoleRepository roleRepository;
  AdminPortalCacheService adminPortalCacheService;

  @Override
  @Transactional(readOnly = true)
  public List<AdminVehicleTypeResponse> getVehicleTypes() {
    return adminPortalCacheService.getVehicleTypes().orElseGet(this::loadVehicleTypes);
  }

  @Override
  @Transactional
  public AdminVehicleTypeResponse createVehicleType(AdminVehicleTypeRequest request) {
    String code = normalizeCode(request.code());
    if (vehicleTypeRepository.existsByCodeIgnoreCase(code)) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Mã loại phương tiện đã tồn tại");
    }

    VehicleType vehicleType =
        VehicleType.builder()
            .name(request.name().trim())
            .code(code)
            .active(request.active() == null || request.active())
            .build();

    AdminVehicleTypeResponse response =
        toVehicleTypeResponse(vehicleTypeRepository.save(vehicleType));
    adminPortalCacheService.evictVehicleTypes();
    return response;
  }

  @Override
  @Transactional
  public AdminVehicleTypeResponse updateVehicleType(UUID id, AdminVehicleTypeRequest request) {
    VehicleType vehicleType = getVehicleType(id);
    String code = normalizeCode(request.code());

    if (vehicleTypeRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Mã loại phương tiện đã tồn tại");
    }

    vehicleType.setName(request.name().trim());
    vehicleType.setCode(code);
    if (request.active() != null) {
      vehicleType.setActive(request.active());
    }

    AdminVehicleTypeResponse response =
        toVehicleTypeResponse(vehicleTypeRepository.save(vehicleType));
    adminPortalCacheService.evictVehicleTypes();
    return response;
  }

  @Override
  @Transactional
  public void deleteVehicleType(UUID id) {
    VehicleType vehicleType = getVehicleType(id);
    vehicleType.setActive(false);
    vehicleType.setDeleted(true);
    vehicleTypeRepository.save(vehicleType);
    adminPortalCacheService.evictVehicleTypes();
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminRoleResponse> getRoles() {
    return roleRepository.findAllByOrderByNameAsc().stream().map(this::toRoleResponse).toList();
  }

  private List<AdminVehicleTypeResponse> loadVehicleTypes() {
    List<AdminVehicleTypeResponse> response =
        vehicleTypeRepository.findAllByDeletedFalseOrderByNameAsc().stream()
            .map(this::toVehicleTypeResponse)
            .toList();
    adminPortalCacheService.saveVehicleTypes(response);
    return response;
  }

  private VehicleType getVehicleType(UUID id) {
    return vehicleTypeRepository
        .findById(id)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND));
  }

  private String normalizeCode(String code) {
    return code.trim().toUpperCase();
  }

  private AdminVehicleTypeResponse toVehicleTypeResponse(VehicleType vehicleType) {
    return AdminVehicleTypeResponse.builder()
        .id(vehicleType.getId())
        .name(vehicleType.getName())
        .code(vehicleType.getCode())
        .active(vehicleType.isActive())
        .createdAt(vehicleType.getCreatedAt())
        .build();
  }

  private AdminRoleResponse toRoleResponse(Role role) {
    return AdminRoleResponse.builder()
        .id(role.getId())
        .name(role.getName())
        .desc(role.getDesc())
        .build();
  }
}
