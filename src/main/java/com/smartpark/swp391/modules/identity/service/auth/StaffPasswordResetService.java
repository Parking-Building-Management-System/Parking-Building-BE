package com.smartpark.swp391.modules.identity.service.auth;

import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.identity.enumType.StaffPasswordResetStatus;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerPasswordResetCompleteRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerPasswordResetRejectRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerPasswordResetResponse;
import java.time.LocalDateTime;
import java.util.UUID;

public interface StaffPasswordResetService {

  void requestReset(String normalizedEmail);

  PageResponse<ManagerPasswordResetResponse> getRequests(
      StaffPasswordResetStatus status,
      String search,
      LocalDateTime from,
      LocalDateTime to,
      int page,
      int size);

  ManagerPasswordResetResponse getRequest(UUID id);

  ManagerPasswordResetResponse complete(
      UUID id, ManagerPasswordResetCompleteRequest request, UUID managerUserId);

  ManagerPasswordResetResponse reject(
      UUID id, ManagerPasswordResetRejectRequest request, UUID managerUserId);
}
