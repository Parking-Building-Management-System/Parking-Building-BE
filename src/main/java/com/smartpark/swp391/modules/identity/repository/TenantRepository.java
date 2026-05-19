package com.smartpark.swp391.modules.identity.repository;

import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.enumType.TenantStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
  boolean existsBySlug(String slug);

  Optional<Tenant> findBySlug(String slug);

  @Query("SELECT COUNT(t) FROM Tenant t WHERE t.status = :status AND t.isDeleted = false")
  long countActiveByStatus(@Param("status") TenantStatus status);
}
