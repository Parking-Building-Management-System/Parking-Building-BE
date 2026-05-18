package com.smartpark.swp391.modules.identity.repository;

import com.smartpark.swp391.modules.identity.entity.Permission;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PermissionRepository extends JpaRepository<Permission, UUID> {

  @Query(
      """
                SELECT DISTINCT p.name FROM Permission p
                JOIN RolePermission rp ON p.id = rp.permission.id
                JOIN UserRole ur ON rp.role.id = ur.role.id
                WHERE ur.user.id = :userId
            """)
  List<String> findPermissionNamesByUserId(@Param("userId") UUID userId);
}
