package com.smartpark.swp391.modules.manager.specification;

import com.smartpark.swp391.modules.identity.entity.StaffPasswordResetRequest;
import com.smartpark.swp391.modules.identity.enumType.StaffPasswordResetStatus;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ManagerPasswordResetRequestSpecifications {

  private ManagerPasswordResetRequestSpecifications() {}

  public static Specification<StaffPasswordResetRequest> filtered(
      UUID tenantId,
      StaffPasswordResetStatus status,
      String search,
      LocalDateTime from,
      LocalDateTime to) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(criteriaBuilder.equal(root.get("tenant").get("id"), tenantId));

      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get("status"), status));
      }

      String normalizedSearch = normalize(search);
      if (normalizedSearch != null) {
        String pattern = "%" + normalizedSearch.toLowerCase(Locale.ROOT) + "%";
        predicates.add(
            criteriaBuilder.or(
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("staffUser").get("fullName")), pattern),
                criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("staffUser").get("username")), pattern),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("requestedEmail")), pattern)));
      }

      if (from != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("requestedAt"), from));
      }
      if (to != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("requestedAt"), to));
      }

      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static String normalize(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
