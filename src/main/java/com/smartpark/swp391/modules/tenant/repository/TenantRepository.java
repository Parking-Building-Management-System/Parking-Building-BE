package com.smartpark.swp391.modules.tenant.repository;

import com.smartpark.swp391.modules.tenant.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long>, JpaSpecificationExecutor<Tenant> {
    // Để trống vì JpaSpecificationExecutor đã cung cấp sẵn hàm tìm kiếm nâng cao kèm phân trang.
}