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

            // Chỉ lấy các tenant chưa bị xóa mềm
            predicates.add(cb.equal(root.get("isDeleted"), false));

            // 1. Nếu có gõ từ khóa vào thanh tìm kiếm chung (tìm theo Tên, Email, hoặc Liên hệ)
            if (StringUtils.hasText(param.getSearch())) {
                String searchPattern = "%" + param.getSearch().toLowerCase() + "%";
                Predicate searchInName = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate searchInEmail = cb.like(cb.lower(root.get("email")), searchPattern);
                Predicate searchInContact = cb.like(cb.lower(root.get("contact")), searchPattern);
                
                // Gom lại thành: WHERE is_deleted = false AND (name LIKE %...% OR email_contact LIKE %...% OR contact LIKE %...%)
                predicates.add(cb.or(searchInName, searchInEmail, searchInContact));
            }

            // 2. Nếu có lọc theo Trạng thái
            if (StringUtils.hasText(param.getStatus())) {
                predicates.add(cb.equal(root.get("status"), param.getStatus().toUpperCase()));
            }

            // Kết hợp toàn bộ các điều kiện trên bằng toán tử AND
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
