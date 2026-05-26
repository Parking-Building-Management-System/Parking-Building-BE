package com.smartpark.swp391.modules.manager.service;

import com.smartpark.swp391.modules.manager.dto.device.DeviceApprovalRequest;
import com.smartpark.swp391.modules.manager.dto.device.ManagerDeviceResponse;
import java.util.List;
import java.util.UUID;

public interface ManagerDeviceApprovalService {
  List<ManagerDeviceResponse> getPendingApprovals();

  ManagerDeviceResponse approve(UUID id, DeviceApprovalRequest request, UUID managerUserId);

  ManagerDeviceResponse reject(UUID id);

  ManagerDeviceResponse revoke(UUID id);
}
