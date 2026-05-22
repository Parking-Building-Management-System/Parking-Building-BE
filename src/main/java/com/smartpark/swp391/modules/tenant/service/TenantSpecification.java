package com.smartpark.swp391.modules.tenant.specification;

import com.smartpark.swp391.modules.tenant.dto.TenantParamRequest;
import com.smartpark.swp391.modules.tenant.entity.Tenant;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import java.util.ArrayList;
import java.util.List;

public class TenantSpecification {

    public static Specification<Tenant> getFilterSpecification(TenantParamRequest param) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Nếu có gõ từ khóa vào thanh tìm kiếm chung
            if (StringUtils.hasText(param.getSearch())) {
                String searchPattern = "%" + param.getSearch().toLowerCase() + "%";
                Predicate searchInName = cb.like(cb.lower(root.get("fullName")), searchPattern);
                Predicate searchInCode = cb.like(cb.lower(root.get("tenantCode")), searchPattern);
                Predicate searchInPhone = cb.like(root.get("phoneNumber"), searchPattern);
                
                // Gom lại thành: WHERE (fullName LIKE %...% OR tenantCode LIKE %...% OR phoneNumber LIKE %...%)
                predicates.add(cb.or(searchInName, searchInCode, searchInPhone));
            }

            // 2. Nếu có lọc theo Số phòng
            if (StringUtils.hasText(param.getApartmentNumber())) {
                predicates.add(cb.equal(root.get("apartmentNumber"), param.getApartmentNumber()));
            }

            // 3. Nếu có lọc theo Trạng thái
            if (StringUtils.hasText(param.getStatus())) {
                predicates.add(cb.equal(root.get("status"), param.getStatus().toUpperCase()));
            }

            // Kết hợp toàn bộ các điều kiện trên bằng toán tử AND
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}