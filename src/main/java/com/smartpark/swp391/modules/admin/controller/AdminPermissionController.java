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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@Tag(
    name = "System Admin Permissions",
    description = "SYSTEM_ADMIN role and permission tree management APIs")
public class AdminPermissionController {

  AdminPermissionService adminPermissionService;

  @GetMapping("/admin/permissions/tree")
  @Operation(
      summary = "Get permission tree",
      description =
          "Actor: SYSTEM_ADMIN. Returns permissions grouped by scope/module/resource/action for"
              + " the admin UI. Read-only; used before editing a role.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permission tree loaded"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required")
  })
  public ResponseEntity<ApiResponse<List<PermissionScopeNode>>> getPermissionTree() {
    return ok("/admin/permissions/tree", adminPermissionService.getPermissionTree());
  }

  @GetMapping("/admin/roles")
  @Operation(
      summary = "List roles",
      description = "Actor: SYSTEM_ADMIN. Lists configured global roles. Read-only.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Roles loaded"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required")
  })
  public ResponseEntity<ApiResponse<List<AdminRoleResponse>>> getRoles() {
    return ok("/admin/roles", adminPermissionService.getRoles());
  }

  @GetMapping("/admin/roles/{roleId}/permissions")
  @Operation(
      summary = "Get permissions for one role",
      description =
          "Actor: SYSTEM_ADMIN. Returns the selected role permission tree. Read-only; role must"
              + " exist.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role permissions loaded"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Role not found")
  })
  public ResponseEntity<ApiResponse<List<PermissionScopeNode>>> getRolePermissions(
      @PathVariable UUID roleId) {
    return ok(
        "/admin/roles/" + roleId + "/permissions",
        adminPermissionService.getRolePermissionTree(roleId));
  }

  @PutMapping("/admin/roles/{roleId}/permissions")
  @Operation(
      summary = "Replace role permissions",
      description =
          "Actor: SYSTEM_ADMIN. Replaces the role-permission links with the supplied permission"
              + " IDs and evicts authorization cache so future JWT checks use the new grants.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
        responseCode = "200",
        description = "Role permissions updated",
        content = @Content(schema = @Schema(implementation = RolePermissionUpdateResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid permission IDs or request body"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Role not found")
  })
  public ResponseEntity<ApiResponse<RolePermissionUpdateResponse>> updateRolePermissions(
      @PathVariable UUID roleId, @Valid @RequestBody RolePermissionUpdateRequest request) {
    return ok(
        "/admin/roles/" + roleId + "/permissions",
        adminPermissionService.replaceRolePermissions(roleId, request));
  }

  @PostMapping("/admin/permissions")
  @Operation(
      summary = "Create permission",
      description =
          "Actor: SYSTEM_ADMIN. Adds a global permission definition that can later be assigned"
              + " to roles.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permission created"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or duplicate permission"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required")
  })
  public ResponseEntity<ApiResponse<PermissionResponse>> createPermission(
      @Valid @RequestBody PermissionRequest request) {
    return ok("/admin/permissions", adminPermissionService.createPermission(request));
  }

  @PutMapping("/admin/permissions/{id}")
  @Operation(
      summary = "Update permission",
      description =
          "Actor: SYSTEM_ADMIN. Edits a global permission definition and clears permission tree"
              + " cache.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permission updated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid or duplicate permission"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Permission not found")
  })
  public ResponseEntity<ApiResponse<PermissionResponse>> updatePermission(
      @PathVariable UUID id, @Valid @RequestBody PermissionRequest request) {
    return ok("/admin/permissions/" + id, adminPermissionService.updatePermission(id, request));
  }

  @DeleteMapping("/admin/permissions/{id}")
  @Operation(
      summary = "Delete permission",
      description =
          "Actor: SYSTEM_ADMIN. Soft-deletes a permission definition after validating it is not"
              + " still assigned where deletion would be unsafe.")
  @ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Permission deleted"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Permission cannot be deleted"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "SYSTEM_ADMIN role required"),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Permission not found")
  })
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
