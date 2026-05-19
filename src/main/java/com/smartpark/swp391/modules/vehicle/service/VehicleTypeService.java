package com.smartpark.swp391.modules.vehicle.service;

import com.smartpark.swp391.modules.vehicle.dto.request.VehicleTypeUpdateRequest;
import com.smartpark.swp391.modules.vehicle.dto.response.VehicleTypeResponse;
import java.util.UUID;

public interface VehicleTypeService {

  VehicleTypeResponse updateVehicleType(UUID id, VehicleTypeUpdateRequest request);

  void deleteVehicleType(UUID id);
}
