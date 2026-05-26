package com.smartpark.swp391.modules.manager.service;

import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskRequest;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskResponse;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskStaffResponse;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskStatusRequest;
import com.smartpark.swp391.modules.manager.dto.kiosk.ManagerKioskUpdateRequest;
import com.smartpark.swp391.modules.operation.enumType.KioskStatus;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
import java.util.List;
import java.util.UUID;

public interface ManagerKioskService {
  List<ManagerKioskResponse> getKiosks(UUID parkingId, KioskStatus status, KioskType type);

  ManagerKioskResponse createKiosk(ManagerKioskRequest request);

  ManagerKioskResponse getKiosk(UUID id);

  ManagerKioskResponse updateKiosk(UUID id, ManagerKioskUpdateRequest request);

  ManagerKioskResponse updateStatus(UUID id, ManagerKioskStatusRequest request);

  void deleteKiosk(UUID id);

  List<ManagerKioskStaffResponse> getStaff(UUID kioskId);

  ManagerKioskStaffResponse assignStaff(UUID kioskId, UUID staffId);

  void unassignStaff(UUID kioskId, UUID staffId);
}
