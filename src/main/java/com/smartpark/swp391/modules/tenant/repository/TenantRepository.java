package com.smartpark.swp391.modules.tenant.repository;

import com.smartpark.swp391.modules.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository("moduleTenantRepository")
public interface TenantRepository extends JpaRepository<Tenant, String>, JpaSpecificationExecutor<Tenant> {
    boolean existsByEmail(String email);
}