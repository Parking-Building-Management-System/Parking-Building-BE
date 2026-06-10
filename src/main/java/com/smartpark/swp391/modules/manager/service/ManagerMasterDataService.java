package com.smartpark.swp391.modules.manager.service;

import com.smartpark.swp391.modules.admin.dto.masterdata.AdminVehicleTypeResponse;
import java.util.List;

public interface ManagerMasterDataService {
  List<AdminVehicleTypeResponse> getActiveVehicleTypes();
}
