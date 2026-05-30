package com.smartpark.swp391.modules.vehicle.service;

import com.smartpark.swp391.modules.vehicle.dto.VehicleTypeCreateRequest;
import com.smartpark.swp391.modules.vehicle.dto.VehicleTypeDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VehicleTypeService {

  VehicleTypeDTO createVehicleType(VehicleTypeCreateRequest request);

  Page<VehicleTypeDTO> getAllVehicleTypes(Pageable pageable, String searchKey);
}
