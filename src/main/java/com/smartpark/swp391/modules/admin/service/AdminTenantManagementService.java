package com.smartpark.swp391.modules.admin.service;

import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.admin.dto.tenant.AdminTenantProvisionRequest;
import com.smartpark.swp391.modules.admin.dto.tenant.AdminTenantStatusResponse;
import com.smartpark.swp391.modules.admin.dto.tenant.AdminTenantSummaryResponse;
import java.util.UUID;

public interface AdminTenantManagementService {
  PageResponse<AdminTenantSummaryResponse> getTenants(int page, int size);

  AdminTenantSummaryResponse provisionTenant(AdminTenantProvisionRequest request);

  AdminTenantStatusResponse toggleTenantStatus(UUID id);
}
