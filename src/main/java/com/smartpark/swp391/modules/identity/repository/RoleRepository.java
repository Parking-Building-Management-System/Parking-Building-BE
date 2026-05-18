package com.smartpark.swp391.modules.identity.repository;

import com.smartpark.swp391.modules.identity.entity.Role;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoleRepository extends JpaRepository<Role, UUID> {

  @Query(
      """
                SELECT r.name FROM Role r
                JOIN UserRole ur ON r.id = ur.role.id
                WHERE ur.user.id = :userId
            """)
  List<String> findRoleNamesByUserId(@Param("userId") UUID userId);
}
