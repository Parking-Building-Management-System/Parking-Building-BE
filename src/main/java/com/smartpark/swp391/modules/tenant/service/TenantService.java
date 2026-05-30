package com.smartpark.swp391.modules.tenant.service;

import com.smartpark.swp391.modules.tenant.dto.TenantCreateRequest;
import com.smartpark.swp391.modules.tenant.dto.TenantParamRequest;
import com.smartpark.swp391.modules.tenant.entity.Tenant;
import org.springframework.data.domain.Page;

public interface TenantService {

    Tenant createTenant(TenantCreateRequest request);

    Page<Tenant> getTenantList(TenantParamRequest param);
}
