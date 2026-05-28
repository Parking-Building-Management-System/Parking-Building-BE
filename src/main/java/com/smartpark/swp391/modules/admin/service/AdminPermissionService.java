package com.smartpark.swp391.modules.admin.service;

import com.smartpark.swp391.modules.admin.dto.masterdata.AdminRoleResponse;
import com.smartpark.swp391.modules.admin.dto.permission.PermissionRequest;
import com.smartpark.swp391.modules.admin.dto.permission.PermissionResponse;
import com.smartpark.swp391.modules.admin.dto.permission.PermissionScopeNode;
import com.smartpark.swp391.modules.admin.dto.permission.RolePermissionUpdateRequest;
import com.smartpark.swp391.modules.admin.dto.permission.RolePermissionUpdateResponse;
import java.util.List;
import java.util.UUID;

public interface AdminPermissionService {
  List<PermissionScopeNode> getPermissionTree();

  List<AdminRoleResponse> getRoles();

  List<PermissionScopeNode> getRolePermissionTree(UUID roleId);

  RolePermissionUpdateResponse replaceRolePermissions(
      UUID roleId, RolePermissionUpdateRequest request);

  PermissionResponse createPermission(PermissionRequest request);

  PermissionResponse updatePermission(UUID id, PermissionRequest request);

  void deletePermission(UUID id);
}
