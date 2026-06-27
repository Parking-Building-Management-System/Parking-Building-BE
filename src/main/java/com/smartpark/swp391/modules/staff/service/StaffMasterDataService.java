package com.smartpark.swp391.modules.staff.service;

import com.smartpark.swp391.modules.admin.dto.masterdata.AdminVehicleTypeResponse;
import java.util.List;

public interface StaffMasterDataService {
  List<AdminVehicleTypeResponse> getActiveVehicleTypes();
}
