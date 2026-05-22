package com.smartpark.swp391.modules.tenant.controller;

import com.smartpark.swp391.modules.tenant.dto.TenantParamRequest;
import com.smartpark.swp391.modules.tenant.entity.Tenant;
import com.smartpark.swp391.modules.tenant.service.TenantService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
@CrossOrigin(origins = "http://localhost:3000") // Mở cổng kết nối cho NextJS chạy ở cổng 3000 kết nối tới
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public ResponseEntity<Page<Tenant>> getAllTenants(TenantParamRequest param) {
        Page<Tenant> tenantPage = tenantService.getTenantList(param);
        return ResponseEntity.ok(tenantPage); // Trả về dữ liệu kèm trạng thái HTTP STATUS 200 OK
    }
}