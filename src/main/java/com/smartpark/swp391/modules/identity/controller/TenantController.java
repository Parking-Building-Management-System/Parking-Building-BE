package com.smartpark.swp391.modules.identity.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.security.annotation.RateLimit;
import com.smartpark.swp391.modules.identity.dto.tenant.request.TenantCreationRequest;
import com.smartpark.swp391.modules.identity.dto.tenant.response.TenantResponse;
import com.smartpark.swp391.modules.identity.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Tag(
    name = "Tenant Management",
    description = "Quản lý khách hàng (Tenant) - Dành cho System Admin")
public class TenantController {

  TenantService tenantService;

  @Operation(
      summary = "Tạo mới khách hàng (Tenant)",
      description =
          "Tạo một không gian làm việc (Tenant) mới trên hệ thống SaaS. Cần cung cấp slug"
              + " (để làm subdomain).")
  @ApiResponses(
      value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Tạo Tenant thành công",
            content = @Content(schema = @Schema(implementation = TenantResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Dữ liệu không hợp lệ / Trùng Slug",
            content = @Content(schema = @Schema(implementation = ApiResponse.class)))
      })
  @PostMapping
  @RateLimit(limit = 5, duration = 30, type = RateLimit.Type.USER_ID)
  public ResponseEntity<ApiResponse<TenantResponse>> createTenant(
      @Valid @RequestBody TenantCreationRequest request) {
    return ResponseEntity.ok(
        ApiResponse.<TenantResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Tạo khách hàng thành công")
            .result(tenantService.createTenant(request))
            .timestamp(Instant.now())
            .build());
  }

  @Operation(summary = "Lấy thông tin chi tiết của Tenant")
  @GetMapping("/{id}")
  @RateLimit(limit = 20, duration = 60, type = RateLimit.Type.USER_ID)
  public ResponseEntity<ApiResponse<TenantResponse>> getTenant(@PathVariable UUID id) {
    return ResponseEntity.ok(
        ApiResponse.<TenantResponse>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Lấy thông tin thành công")
            .result(tenantService.getTenantById(id))
            .timestamp(Instant.now())
            .build());
  }

  @Operation(
      summary = "Khóa mõm Tenant (Suspend)",
      description =
          "Tạm dừng hoạt động của Tenant. Hệ thống sẽ tự động kick toàn bộ User thuộc"
              + " Tenant này khỏi Redis.")
  @PatchMapping("/{id}/suspend")
  @RateLimit(limit = 5, duration = 30, type = RateLimit.Type.USER_ID)
  public ResponseEntity<ApiResponse<Void>> suspendTenant(@PathVariable UUID id) {
    tenantService.suspendTenant(id);
    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Đã đình chỉ hoạt động khách hàng")
            .timestamp(Instant.now())
            .build());
  }

  @Operation(summary = "Xóa mềm Tenant")
  @DeleteMapping("/{id}")
  @RateLimit(limit = 5, duration = 30, type = RateLimit.Type.USER_ID)
  public ResponseEntity<ApiResponse<Void>> deleteTenant(@PathVariable UUID id) {
    tenantService.deleteTenant(id);
    return ResponseEntity.ok(
        ApiResponse.<Void>builder()
            .code(ErrorCode.SUCCESS.getCode())
            .message("Đã xóa khách hàng")
            .timestamp(Instant.now())
            .build());
  }
}
