package com.smartpark.swp391.modules.staff.service;

import com.smartpark.swp391.modules.staff.dto.StaffWorkContextResponse;
import java.util.Optional;
import java.util.UUID;

public interface StaffWorkContextService {
  Optional<StaffWorkContextResponse> resolveCurrentContext();

  Optional<StaffWorkContextResponse> resolveContext(UUID sessionId, UUID userId, UUID tenantId);

  StaffWorkContextResponse requireCurrentContext();
}
