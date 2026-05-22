package com.smartpark.swp391.modules.tenant.service;

import com.smartpark.swp391.modules.tenant.dto.TenantParamRequest;
import com.smartpark.swp391.modules.tenant.entity.Tenant;
import com.smartpark.swp391.modules.tenant.repository.TenantRepository;
import com.smartpark.swp391.modules.tenant.specification.TenantSpecification;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Page<Tenant> getTenantList(TenantParamRequest param) {
        // 1. Tạo đối tượng xử lý sắp xếp dựa vào tham số FE truyền lên
        Sort sort = param.getSortDir().equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(param.getSortBy()).ascending()
                : Sort.by(param.getSortBy()).descending();

        // 2. Tạo đối tượng phân trang chứa: số trang, kích thước trang, cấu hình sort
        Pageable pageable = PageRequest.of(param.getPage(), param.getSize(), sort);

        // 3. Lấy các điều kiện lọc động từ file Specification
        Specification<Tenant> spec = TenantSpecification.getFilterSpecification(param);

        // 4. Gọi xuống DB và trả về kết quả dạng Page (bao gồm danh sách và các thông số tổng số trang)
        return tenantRepository.findAll(spec, pageable);
    }
}