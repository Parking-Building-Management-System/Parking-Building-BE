package com.smartpark.swp391.modules.admin.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.modules.admin.dto.masterdata.AdminRoleResponse;
import com.smartpark.swp391.modules.admin.dto.permission.PermissionRequest;
import com.smartpark.swp391.modules.admin.dto.permission.PermissionResponse;
import com.smartpark.swp391.modules.admin.dto.permission.PermissionScopeNode;
import com.smartpark.swp391.modules.admin.dto.permission.RolePermissionUpdateRequest;
import com.smartpark.swp391.modules.admin.dto.permission.RolePermissionUpdateResponse;
import com.smartpark.swp391.modules.admin.service.AdminPermissionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Permissions", description = "SYSTEM_ADMIN roles and permissions APIs")
public class AdminPermissionController {

  AdminPermissionService adminPermissionService;

  @GetMapping("/admin/permissions/tree")
  public ResponseEntity<ApiResponse<List<PermissionScopeNode>>> getPermissionTree() {
    return ok("/admin/permissions/tree", adminPermissionService.getPermissionTree());
  }

  @GetMapping("/admin/roles")
  public ResponseEntity<ApiResponse<List<AdminRoleResponse>>> getRoles() {
    return ok("/admin/roles", adminPermissionService.getRoles());
  }

  @GetMapping("/admin/roles/{roleId}/permissions")
  public ResponseEntity<ApiResponse<List<PermissionScopeNode>>> getRolePermissions(
      @PathVariable UUID roleId) {
    return ok(
        "/admin/roles/" + roleId + "/permissions",
        adminPermissionService.getRolePermissionTree(roleId));
  }

  @PutMapping("/admin/roles/{roleId}/permissions")
  public ResponseEntity<ApiResponse<RolePermissionUpdateResponse>> updateRolePermissions(
      @PathVariable UUID roleId, @Valid @RequestBody RolePermissionUpdateRequest request) {
    return ok(
        "/admin/roles/" + roleId + "/permissions",
        adminPermissionService.replaceRolePermissions(roleId, request));
  }

  @PostMapping("/admin/permissions")
  public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
      @Valid @RequestBody PermissionRequest request) {
    return ok("/admin/permissions", adminPermissionService.createPermission(request));
  }

  @PutMapping("/admin/permissions/{id}")
  public ResponseEntity<ApiResponse<PermissionResponse>> updatePermission(
      @PathVariable UUID id, @Valid @RequestBody PermissionRequest request) {
    return ok("/admin/permissions/" + id, adminPermissionService.updatePermission(id, request));
  }

  @DeleteMapping("/admin/permissions/{id}")
  public ResponseEntity<ApiResponse<Void>> deletePermission(@PathVariable UUID id) {
    adminPermissionService.deletePermission(id);
    return ok("/admin/permissions/" + id, null);
  }

  private <T> ResponseEntity<ApiResponse<T>> ok(String path, T result) {
    return ResponseEntity.ok(
        ApiResponse.<T>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(result)
            .timestamp(Instant.now())
            .path(path)
            .build());
  }
}
