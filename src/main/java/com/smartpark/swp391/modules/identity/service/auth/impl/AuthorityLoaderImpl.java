package com.smartpark.swp391.modules.identity.service.auth.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.cached.redis.model.SessionAuthzCache;
import com.smartpark.swp391.modules.identity.repository.PermissionRepository;
import com.smartpark.swp391.modules.identity.repository.RoleRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.identity.service.auth.AuthorityLoader;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AuthorityLoaderImpl implements AuthorityLoader {

  UserRepository userRepository;
  RoleRepository roleRepository;
  PermissionRepository permissionRepository;

  @Override
  @Transactional(readOnly = true)
  public SessionAuthzCache load(UUID userId) {

    // 1. Get Tenant ID
    UUID tenantId =
        userRepository
            .findTenantIdByUserId(userId)
            .orElseThrow(
                () -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Không tìm thấy khách hàng"));

    // 2. Get Roles
    List<String> roles = roleRepository.findRoleNamesByUserId(userId);

    // 3. Get Permissions
    List<String> permissions = permissionRepository.findPermissionNamesByUserId(userId);

    // 4. Push Cache
    return new SessionAuthzCache(
        userId,
        tenantId,
        roles != null ? roles : List.of(),
        permissions != null ? permissions : List.of());
  }
}
