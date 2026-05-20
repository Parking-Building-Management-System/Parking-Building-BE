package com.smartpark.swp391.modules.admin.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.admin.dto.tenant.AdminTenantProvisionRequest;
import com.smartpark.swp391.modules.admin.dto.tenant.AdminTenantStatusResponse;
import com.smartpark.swp391.modules.admin.dto.tenant.AdminTenantSummaryResponse;
import com.smartpark.swp391.modules.admin.service.AdminTenantManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping({"/admin/tenants", "/api/v1/admin/tenants"})
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Tenants", description = "SYSTEM_ADMIN tenant management portal APIs")
public class AdminTenantManagementController {

  AdminTenantManagementService adminTenantManagementService;

  @GetMapping
  @Operation(
      summary = "List tenants",
      description = "Returns a paginated list of tenant workspaces for the SYSTEM_ADMIN portal.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "Tenants loaded successfully",
      content = @Content(schema = @Schema(implementation = PageResponse.class)))
  public ResponseEntity<ApiResponse<PageResponse<AdminTenantSummaryResponse>>> getTenants(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return ResponseEntity.ok(
        ApiResponse.<PageResponse<AdminTenantSummaryResponse>>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message(ErrorCode.SUCCESS.getDefaultMessage())
            .result(adminTenantManagementService.getTenants(page, size))
            .timestamp(Instant.now())
            .path("/admin/tenants")
            .build());
  }

  @PostMapping
  @Operation(
      summary = "Provision tenant",
      description =
          "Creates a tenant and provisions the initial PARKING_MANAGER administrator user from"
              + " company name, manager email, and initial password.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "Tenant provisioned successfully",
      content = @Content(schema = @Schema(implementation = AdminTenantSummaryResponse.class)))
  public ResponseEntity<ApiResponse<AdminTenantSummaryResponse>> provisionTenant(
      @Valid @RequestBody AdminTenantProvisionRequest request) {
    return ResponseEntity.ok(
        ApiResponse.<AdminTenantSummaryResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Tạo tenant thành công")
            .result(adminTenantManagementService.provisionTenant(request))
            .timestamp(Instant.now())
            .path("/admin/tenants")
            .build());
  }

  @PatchMapping("/{id}/status")
  @Operation(
      summary = "Toggle tenant status",
      description =
          "Toggles a tenant between ACTIVE and SUSPENDED. When suspended, all active sessions"
              + " under the tenant are revoked immediately and tenant cache keys are evicted.")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "200",
      description = "Tenant status updated successfully",
      content = @Content(schema = @Schema(implementation = AdminTenantStatusResponse.class)))
  public ResponseEntity<ApiResponse<AdminTenantStatusResponse>> toggleTenantStatus(
      @PathVariable UUID id) {
    return ResponseEntity.ok(
        ApiResponse.<AdminTenantStatusResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Cập nhật trạng thái tenant thành công")
            .result(adminTenantManagementService.toggleTenantStatus(id))
            .timestamp(Instant.now())
            .path("/admin/tenants/" + id + "/status")
            .build());
  }
}
