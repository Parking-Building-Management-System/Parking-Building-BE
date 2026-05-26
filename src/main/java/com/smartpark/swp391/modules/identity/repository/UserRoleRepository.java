package com.smartpark.swp391.modules.identity.repository;

import com.smartpark.swp391.modules.identity.entity.UserRole;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

  @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role WHERE ur.user.id = :userId")
  List<UserRole> findAllByUserIdWithRole(@Param("userId") UUID userId);
}
