package com.smartpark.swp391.modules.identity.repository;

import com.smartpark.swp391.modules.identity.entity.RolePermission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {

  @Query(
      """
          SELECT rp
          FROM RolePermission rp
          JOIN FETCH rp.permission p
          WHERE rp.role.id = :roleId
            AND p.deleted = false
            AND p.status = 'ACTIVE'
          """)
  List<RolePermission> findActiveByRoleId(@Param("roleId") UUID roleId);

  long countByPermission_Id(UUID permissionId);

  @Modifying
  @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId")
  int deleteByRoleId(@Param("roleId") UUID roleId);
}
