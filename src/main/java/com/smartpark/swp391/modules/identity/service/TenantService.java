package com.smartpark.swp391.modules.identity.service;

import com.smartpark.swp391.modules.identity.dto.tenant.request.TenantCreationRequest;
import com.smartpark.swp391.modules.identity.dto.tenant.response.TenantResponse;
import com.smartpark.swp391.modules.identity.enumType.TenantStatus;
import java.util.UUID;

public interface TenantService {
  TenantResponse createTenant(TenantCreationRequest request);

  TenantResponse getTenantById(UUID id);

  TenantResponse updateTenantStatus(UUID id, TenantStatus status);

  void suspendTenant(UUID id);

  void deleteTenant(UUID id);
}
