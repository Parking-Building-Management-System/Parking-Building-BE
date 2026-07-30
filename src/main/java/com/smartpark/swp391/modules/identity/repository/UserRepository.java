package com.smartpark.swp391.modules.identity.repository;

import com.smartpark.swp391.modules.identity.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

  @Query("SELECT u.tenant.id FROM User u WHERE u.id = :userId")
  Optional<UUID> findTenantIdByUserId(@Param("userId") UUID userId);

  Optional<User> findByUsername(String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT u FROM User u WHERE u.username = :username")
  Optional<User> findByUsernameForUpdate(@Param("username") String username);

  boolean existsByUsername(String username);

  @Query(
      """
          SELECT u
          FROM User u
          JOIN UserRole ur ON ur.user.id = u.id
          JOIN Role r ON r.id = ur.role.id
          WHERE u.id = :id
            AND u.tenant.id = :tenantId
            AND r.name = :roleName
          """)
  Optional<User> findTenantUserByIdAndRole(
      @Param("id") UUID id, @Param("tenantId") UUID tenantId, @Param("roleName") String roleName);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      """
          SELECT u
          FROM User u
          WHERE u.id = :id
            AND u.tenant.id = :tenantId
            AND EXISTS (
              SELECT 1
              FROM UserRole ur
              JOIN ur.role r
              WHERE ur.user.id = u.id
                AND r.name = :roleName
            )
          """)
  Optional<User> findTenantUserByIdAndRoleForUpdate(
      @Param("id") UUID id, @Param("tenantId") UUID tenantId, @Param("roleName") String roleName);
}
