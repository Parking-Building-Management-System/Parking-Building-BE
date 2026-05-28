package com.smartpark.swp391.modules.admin.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.infrastructure.cached.redis.service.AdminPortalCacheService;
import com.smartpark.swp391.modules.admin.dto.masterdata.AdminRoleResponse;
import com.smartpark.swp391.modules.admin.dto.permission.PermissionActionNode;
import com.smartpark.swp391.modules.admin.dto.permission.PermissionLabelNode;
import com.smartpark.swp391.modules.admin.dto.permission.PermissionModuleNode;
import com.smartpark.swp391.modules.admin.dto.permission.PermissionRequest;
import com.smartpark.swp391.modules.admin.dto.permission.PermissionResourceNode;
import com.smartpark.swp391.modules.admin.dto.permission.PermissionResponse;
import com.smartpark.swp391.modules.admin.dto.permission.PermissionScopeNode;
import com.smartpark.swp391.modules.admin.dto.permission.RolePermissionUpdateRequest;
import com.smartpark.swp391.modules.admin.dto.permission.RolePermissionUpdateResponse;
import com.smartpark.swp391.modules.admin.service.AdminPermissionService;
import com.smartpark.swp391.modules.identity.entity.Permission;
import com.smartpark.swp391.modules.identity.entity.Role;
import com.smartpark.swp391.modules.identity.entity.RolePermission;
import com.smartpark.swp391.modules.identity.repository.PermissionRepository;
import com.smartpark.swp391.modules.identity.repository.RolePermissionRepository;
import com.smartpark.swp391.modules.identity.repository.RoleRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AdminPermissionServiceImpl implements AdminPermissionService {

  PermissionRepository permissionRepository;
  RoleRepository roleRepository;
  RolePermissionRepository rolePermissionRepository;
  AdminPortalCacheService cacheService;

  @Override
  @Transactional(readOnly = true)
  public List<PermissionScopeNode> getPermissionTree() {
    return cacheService.getPermissionTree().orElseGet(this::loadPermissionTree);
  }

  @Override
  @Transactional(readOnly = true)
  public List<AdminRoleResponse> getRoles() {
    return roleRepository.findAllByOrderByNameAsc().stream().map(this::toRoleResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<PermissionScopeNode> getRolePermissionTree(UUID roleId) {
    requireRole(roleId);
    return cacheService.getRolePermissionTree(roleId).orElseGet(() -> loadRolePermissionTree(roleId));
  }

  @Override
  @Transactional
  public RolePermissionUpdateResponse replaceRolePermissions(
      UUID roleId, RolePermissionUpdateRequest request) {
    Role role = requireRole(roleId);
    Set<UUID> permissionIds = new LinkedHashSet<>(request.permissionIds());
    List<Permission> permissions = permissionRepository.findAllById(permissionIds);
    if (permissions.size() != permissionIds.size()
        || permissions.stream().anyMatch(permission -> permission.isDeleted())) {
      throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "One or more permissions were not found");
    }

    rolePermissionRepository.deleteByRoleId(roleId);
    List<RolePermission> rolePermissions =
        permissions.stream()
            .map(permission -> rolePermission(role, permission))
            .toList();
    rolePermissionRepository.saveAll(rolePermissions);
    cacheService.evictRolePermissionTree(roleId);
    return RolePermissionUpdateResponse.builder()
        .roleId(roleId)
        .permissionCount(rolePermissions.size())
        .build();
  }

  @Override
  @Transactional
  public PermissionResponse createPermission(PermissionRequest request) {
    NormalizedPermission normalized = normalize(request);
    if (permissionRepository
        .existsByDeletedFalseAndScopeIgnoreCaseAndModuleIgnoreCaseAndResourceIgnoreCaseAndLabelIgnoreCaseAndActionIgnoreCase(
            normalized.scope(),
            normalized.module(),
            normalized.resource(),
            normalized.label(),
            normalized.action())) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Permission definition already exists");
    }

    Permission permission =
        Permission.builder()
            .name(permissionName(normalized))
            .scope(normalized.scope())
            .module(normalized.module())
            .resource(normalized.resource())
            .label(normalized.label())
            .action(normalized.action())
            .description(normalized.description())
            .status(normalized.status())
            .deleted(false)
            .build();
    Permission saved = permissionRepository.save(permission);
    cacheService.evictPermissionTree();
    return toPermissionResponse(saved);
  }

  @Override
  @Transactional
  public PermissionResponse updatePermission(UUID id, PermissionRequest request) {
    Permission permission = requirePermission(id);
    NormalizedPermission normalized = normalize(request);
    if (permissionRepository
        .existsByDeletedFalseAndScopeIgnoreCaseAndModuleIgnoreCaseAndResourceIgnoreCaseAndLabelIgnoreCaseAndActionIgnoreCaseAndIdNot(
            normalized.scope(),
            normalized.module(),
            normalized.resource(),
            normalized.label(),
            normalized.action(),
            id)) {
      throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Permission definition already exists");
    }

    permission.setName(permissionName(normalized));
    permission.setScope(normalized.scope());
    permission.setModule(normalized.module());
    permission.setResource(normalized.resource());
    permission.setLabel(normalized.label());
    permission.setAction(normalized.action());
    permission.setDescription(normalized.description());
    permission.setStatus(normalized.status());
    Permission saved = permissionRepository.save(permission);
    evictPermissionCachesFor(id);
    return toPermissionResponse(saved);
  }

  @Override
  @Transactional
  public void deletePermission(UUID id) {
    Permission permission = requirePermission(id);
    if (rolePermissionRepository.countByPermission_Id(id) > 0) {
      throw new ApiException(ErrorCode.FORBIDDEN_ACTION, "Permission is assigned to one or more roles");
    }
    permission.setDeleted(true);
    permission.setStatus("INACTIVE");
    permissionRepository.save(permission);
    evictPermissionCachesFor(id);
  }

  private List<PermissionScopeNode> loadPermissionTree() {
    List<PermissionScopeNode> tree = buildTree(activePermissions(), Set.of(), false);
    cacheService.savePermissionTree(tree);
    return tree;
  }

  private RolePermission rolePermission(Role role, Permission permission) {
    RolePermission rolePermission = new RolePermission();
    rolePermission.setRole(role);
    rolePermission.setPermission(permission);
    return rolePermission;
  }

  private List<PermissionScopeNode> loadRolePermissionTree(UUID roleId) {
    Set<UUID> selectedIds =
        rolePermissionRepository.findActiveByRoleId(roleId).stream()
            .map(rolePermission -> rolePermission.getPermission().getId())
            .collect(java.util.stream.Collectors.toSet());
    List<PermissionScopeNode> tree = buildTree(activePermissions(), selectedIds, true);
    cacheService.saveRolePermissionTree(roleId, tree);
    return tree;
  }

  private List<Permission> activePermissions() {
    return permissionRepository
        .findAllByDeletedFalseAndStatusOrderByScopeAscModuleAscResourceAscLabelAscActionAsc(
            "ACTIVE");
  }

  private List<PermissionScopeNode> buildTree(
      List<Permission> permissions, Set<UUID> selectedIds, boolean includeSelected) {
    Map<String, ScopeGroup> scopes = new LinkedHashMap<>();
    for (Permission permission : permissions) {
      ScopeGroup scope =
          scopes.computeIfAbsent(permission.getScope(), key -> new ScopeGroup(key));
      ModuleGroup module =
          scope.modules.computeIfAbsent(permission.getModule(), key -> new ModuleGroup(key));
      ResourceGroup resource =
          module.resources.computeIfAbsent(permission.getResource(), key -> new ResourceGroup(key));
      LabelGroup label = resource.labels.computeIfAbsent(permission.getLabel(), key -> new LabelGroup(key));
      label.actions.add(
          PermissionActionNode.builder()
              .id(permission.getId())
              .action(permission.getAction())
              .selected(includeSelected ? selectedIds.contains(permission.getId()) : null)
              .build());
    }
    return scopes.values().stream().map(ScopeGroup::toNode).toList();
  }

  private Role requireRole(UUID roleId) {
    return roleRepository
        .findById(roleId)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Role not found"));
  }

  private Permission requirePermission(UUID id) {
    return permissionRepository
        .findByIdAndDeletedFalse(id)
        .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Permission not found"));
  }

  private void evictPermissionCachesFor(UUID permissionId) {
    cacheService.evictPermissionTree();
    rolePermissionRepository
        .findAll()
        .stream()
        .filter(rp -> permissionId.equals(rp.getPermission().getId()))
        .map(rp -> rp.getRole().getId())
        .distinct()
        .forEach(cacheService::evictRolePermissionTree);
  }

  private NormalizedPermission normalize(PermissionRequest request) {
    return new NormalizedPermission(
        upper(request.scope()),
        upper(request.module()),
        upper(request.resource()),
        request.label().trim(),
        upper(request.action()),
        blankToNull(request.description()),
        request.status() == null || request.status().isBlank() ? "ACTIVE" : upper(request.status()));
  }

  private String upper(String value) {
    return value.trim().toUpperCase(Locale.ROOT);
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private String permissionName(NormalizedPermission permission) {
    return String.join(
        ":", permission.scope(), permission.module(), permission.resource(), permission.action());
  }

  private AdminRoleResponse toRoleResponse(Role role) {
    return AdminRoleResponse.builder()
        .id(role.getId())
        .name(role.getName())
        .desc(role.getDesc())
        .build();
  }

  private PermissionResponse toPermissionResponse(Permission permission) {
    return PermissionResponse.builder()
        .id(permission.getId())
        .name(permission.getName())
        .scope(permission.getScope())
        .module(permission.getModule())
        .resource(permission.getResource())
        .label(permission.getLabel())
        .action(permission.getAction())
        .description(permission.getDescription())
        .status(permission.getStatus())
        .build();
  }

  private record NormalizedPermission(
      String scope,
      String module,
      String resource,
      String label,
      String action,
      String description,
      String status) {}

  private static class ScopeGroup {
    final String scope;
    final Map<String, ModuleGroup> modules = new LinkedHashMap<>();

    ScopeGroup(String scope) {
      this.scope = scope;
    }

    PermissionScopeNode toNode() {
      return PermissionScopeNode.builder()
          .scope(scope)
          .modules(modules.values().stream().map(ModuleGroup::toNode).toList())
          .build();
    }
  }

  private static class ModuleGroup {
    final String module;
    final Map<String, ResourceGroup> resources = new LinkedHashMap<>();

    ModuleGroup(String module) {
      this.module = module;
    }

    PermissionModuleNode toNode() {
      return PermissionModuleNode.builder()
          .module(module)
          .resources(resources.values().stream().map(ResourceGroup::toNode).toList())
          .build();
    }
  }

  private static class ResourceGroup {
    final String resource;
    final Map<String, LabelGroup> labels = new LinkedHashMap<>();

    ResourceGroup(String resource) {
      this.resource = resource;
    }

    PermissionResourceNode toNode() {
      return PermissionResourceNode.builder()
          .resource(resource)
          .labels(labels.values().stream().map(LabelGroup::toNode).toList())
          .build();
    }
  }

  private static class LabelGroup {
    final String label;
    final List<PermissionActionNode> actions = new java.util.ArrayList<>();

    LabelGroup(String label) {
      this.label = label;
    }

    PermissionLabelNode toNode() {
      return PermissionLabelNode.builder().label(label).actions(actions).build();
    }
  }
}
