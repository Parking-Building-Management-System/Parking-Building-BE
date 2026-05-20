package com.smartpark.swp391.modules.identity.repository;

import com.smartpark.swp391.modules.identity.entity.UserRole;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {}
