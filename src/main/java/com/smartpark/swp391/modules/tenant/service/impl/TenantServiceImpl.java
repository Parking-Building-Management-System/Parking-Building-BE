package com.smartpark.swp391.modules.tenant.service.impl;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.common.exception.ErrorCode;
import com.smartpark.swp391.modules.tenant.dto.TenantCreateRequest;
import com.smartpark.swp391.modules.tenant.dto.TenantParamRequest;
import com.smartpark.swp391.modules.tenant.entity.Tenant;
import com.smartpark.swp391.modules.tenant.repository.TenantRepository;
import com.smartpark.swp391.modules.tenant.service.TenantService;
import com.smartpark.swp391.modules.tenant.specification.TenantSpecification;
import java.text.Normalizer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service("moduleTenantService")
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;

    @Override
    @Transactional
    public Tenant createTenant(TenantCreateRequest request) {
        log.info("Bắt đầu tạo mới Tenant với email: {}", request.email());

        if (tenantRepository.existsByEmail(request.email())) {
            log.warn("Tạo Tenant thất bại - Email đã tồn tại: {}", request.email());
            throw new ApiException(ErrorCode.DUPLICATE_RESOURCE, "Email đã tồn tại trên hệ thống");
        }

        String baseSlug = slugify(request.name());
        String slug = baseSlug + "-" + UUID.randomUUID().toString().substring(0, 8);

        Tenant tenant =
                Tenant.builder()
                        .name(request.name())
                        .email(request.email())
                        .contact(request.contact())
                        .slug(slug)
                        .status("ACTIVE")
                        .isDeleted(false)
                        .build();

        tenant = tenantRepository.save(tenant);
        log.info("Tạo Tenant thành công - ID: [{}], Slug: {}", tenant.getId(), tenant.getSlug());
        return tenant;
    }

    @Override
    public Page<Tenant> getTenantList(TenantParamRequest param) {
        Sort sort =
                param.getSortDir().equalsIgnoreCase(Sort.Direction.ASC.name())
                        ? Sort.by(param.getSortBy()).ascending()
                        : Sort.by(param.getSortBy()).descending();

        Pageable pageable = PageRequest.of(param.getPage(), param.getSize(), sort);
        Specification<Tenant> spec = TenantSpecification.getFilterSpecification(param);

        return tenantRepository.findAll(spec, pageable);
    }

    private String slugify(String input) {
        if (input == null) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("đ", "d")
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}
