package com.smartpark.swp391.modules.identity.repository;

import com.smartpark.swp391.modules.identity.entity.StaffPasswordResetRequest;
import com.smartpark.swp391.modules.identity.enumType.StaffPasswordResetStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StaffPasswordResetRequestRepository
    extends JpaRepository<StaffPasswordResetRequest, UUID>,
        JpaSpecificationExecutor<StaffPasswordResetRequest> {

  boolean existsByStaffUserIdAndStatus(UUID staffUserId, StaffPasswordResetStatus status);

  @Override
  @EntityGraph(attributePaths = {"staffUser", "reviewedByManager"})
  Page<StaffPasswordResetRequest> findAll(
      Specification<StaffPasswordResetRequest> specification, Pageable pageable);

  @EntityGraph(attributePaths = {"staffUser", "reviewedByManager"})
  @Query(
      """
      SELECT request
      FROM StaffPasswordResetRequest request
      WHERE request.tenant.id = :tenantId
        AND request.id = :id
      """)
  Optional<StaffPasswordResetRequest> findTenantRequest(
      @Param("tenantId") UUID tenantId, @Param("id") UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT request
      FROM StaffPasswordResetRequest request
      WHERE request.tenant.id = :tenantId
        AND request.id = :id
      """)
  Optional<StaffPasswordResetRequest> findTenantRequestForUpdate(
      @Param("tenantId") UUID tenantId, @Param("id") UUID id);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
      SELECT request
      FROM StaffPasswordResetRequest request
      WHERE request.tenant.id = :tenantId
        AND request.staffUser.id = :staffUserId
        AND request.status =
          com.smartpark.swp391.modules.identity.enumType.StaffPasswordResetStatus.PENDING
      """)
  Optional<StaffPasswordResetRequest> findPendingForStaffForUpdate(
      @Param("tenantId") UUID tenantId, @Param("staffUserId") UUID staffUserId);
}
