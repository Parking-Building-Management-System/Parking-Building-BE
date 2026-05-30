package com.smartpark.swp391.modules.tenant.controller;

import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.common.response.ApiResponse;
import com.smartpark.swp391.common.response.PageResponse;
import com.smartpark.swp391.modules.tenant.dto.TenantCreateRequest;
import com.smartpark.swp391.modules.tenant.dto.TenantParamRequest;
import com.smartpark.swp391.modules.tenant.entity.Tenant;
import com.smartpark.swp391.modules.tenant.service.TenantService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("moduleTenantController")
@RequestMapping("/api/v1/tenants")
@CrossOrigin(origins = "http://localhost:3000")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Tenant>> createTenant(
            @Valid @RequestBody TenantCreateRequest request) {
        Tenant createdTenant = tenantService.createTenant(request);
        ApiResponse<Tenant> response =
                ApiResponse.<Tenant>builder()
                        .code(ErrorCode.SUCCESS.getCode())
                        .message("Tạo mới khách hàng (Tenant) thành công")
                        .result(createdTenant)
                        .timestamp(Instant.now())
                        .build();
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<Tenant>>> getAllTenants(
            @ModelAttribute TenantParamRequest param) {
        Page<Tenant> tenantPage = tenantService.getTenantList(param);
        PageResponse<Tenant> pageResponse =
                new PageResponse<>(
                        tenantPage.getContent(),
                        tenantPage.getNumber(),
                        tenantPage.getSize(),
                        tenantPage.getTotalElements(),
                        tenantPage.getTotalPages());

        ApiResponse<PageResponse<Tenant>> response =
                ApiResponse.<PageResponse<Tenant>>builder()
                        .code(ErrorCode.SUCCESS.getCode())
                        .message("Lấy danh sách khách hàng thành công")
                        .result(pageResponse)
                        .timestamp(Instant.now())
                        .build();
        return ResponseEntity.ok(response);
    }
}
