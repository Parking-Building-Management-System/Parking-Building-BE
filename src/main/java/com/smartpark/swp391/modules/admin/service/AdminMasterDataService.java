package com.smartpark.swp391.modules.admin.service;

import com.smartpark.swp391.modules.admin.dto.masterdata.AdminRoleResponse;
import com.smartpark.swp391.modules.admin.dto.masterdata.AdminVehicleTypeRequest;
import com.smartpark.swp391.modules.admin.dto.masterdata.AdminVehicleTypeResponse;
import java.util.List;
import java.util.UUID;

public interface AdminMasterDataService {
  List<AdminVehicleTypeResponse> getVehicleTypes();

  AdminVehicleTypeResponse createVehicleType(AdminVehicleTypeRequest request);

  AdminVehicleTypeResponse updateVehicleType(UUID id, AdminVehicleTypeRequest request);

  void deleteVehicleType(UUID id);

  List<AdminRoleResponse> getRoles();
}
