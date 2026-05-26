package com.smartpark.swp391.modules.manager.service;

import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffCreateRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffPasswordResetRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffResponse;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffStatusRequest;
import com.smartpark.swp391.modules.manager.dto.staff.ManagerStaffUpdateRequest;
import java.util.UUID;

public interface ManagerStaffService {

  PageResponse<ManagerStaffResponse> getStaff(String search, UserStatus status, int page, int size);

  ManagerStaffResponse createStaff(ManagerStaffCreateRequest request, UUID managerUserId);

  ManagerStaffResponse getStaffById(UUID id);

  ManagerStaffResponse updateStaff(UUID id, ManagerStaffUpdateRequest request);

  ManagerStaffResponse updateStatus(UUID id, ManagerStaffStatusRequest request);

  ManagerStaffResponse resetPassword(UUID id, ManagerStaffPasswordResetRequest request);
}
