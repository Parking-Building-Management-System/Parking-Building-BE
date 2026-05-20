package com.smartpark.swp391.modules.identity.service.tenant;

import com.smartpark.swp391.modules.identity.dto.tenant.request.TenantCreationRequest;
import com.smartpark.swp391.modules.identity.dto.tenant.response.TenantResponse;
import java.util.UUID;

public interface TenantService {
  TenantResponse createTenant(TenantCreationRequest request);

  TenantResponse getTenantById(UUID id);

  void suspendTenant(UUID id);

  void deleteTenant(UUID id);
}
