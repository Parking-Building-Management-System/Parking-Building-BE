package com.smartpark.swp391.modules.manager.specification;

import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.entity.UserRole;
import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ManagerStaffSpecifications {

  private ManagerStaffSpecifications() {}

  public static Specification<User> tenantStaff(UUID tenantId, String roleName, UserStatus status) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(criteriaBuilder.equal(root.get("tenant").get("id"), tenantId));
      predicates.add(criteriaBuilder.isFalse(root.get("isDeleted")));
      predicates.add(hasRole(roleName).toPredicate(root, query, criteriaBuilder));

      if (status != null) {
        predicates.add(criteriaBuilder.equal(root.get("status"), status));
      }

      return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
    };
  }

  public static Specification<User> search(String search) {
    String normalized = normalizeSearch(search);
    if (normalized == null) {
      return (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();
    }

    return (root, query, criteriaBuilder) -> {
      String pattern = "%" + normalized.toLowerCase(Locale.ROOT) + "%";
      return criteriaBuilder.or(
          criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), pattern),
          criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), pattern),
          criteriaBuilder.like(criteriaBuilder.lower(root.get("phone")), pattern));
    };
  }

  private static Specification<User> hasRole(String roleName) {
    return (root, query, criteriaBuilder) -> {
      Subquery<UUID> subquery = query.subquery(UUID.class);
      var userRole = subquery.from(UserRole.class);
      var role = userRole.join("role");
      subquery.select(userRole.get("user").get("id"));
      subquery.where(
          criteriaBuilder.equal(userRole.get("user").get("id"), root.get("id")),
          criteriaBuilder.equal(role.get("name"), roleName));
      return criteriaBuilder.exists(subquery);
    };
  }

  private static String normalizeSearch(String search) {
    if (search == null || search.isBlank()) {
      return null;
    }
    return search.trim();
  }
}
