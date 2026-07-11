package com.smartpark.swp391.modules.penalty.repository;

import com.smartpark.swp391.modules.penalty.entity.PenaltyCase;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class PenaltyCaseRepositoryImpl implements PenaltyCaseRepositoryCustom {

  private final EntityManager entityManager;

  @Override
  public List<PenaltyCase> findViolationReports(
      UUID tenantId,
      UUID parkingId,
      PenaltyCaseStatus status,
      String reportedPlate,
      LocalDateTime fromDate,
      LocalDateTime toDate) {
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<PenaltyCase> query = criteriaBuilder.createQuery(PenaltyCase.class);
    Root<PenaltyCase> root = query.from(PenaltyCase.class);
    root.fetch("victimSession", JoinType.LEFT);
    root.fetch("offenderSession", JoinType.LEFT);
    root.fetch("reportedSlot", JoinType.LEFT);
    root.fetch("reassignedSlot", JoinType.LEFT);
    root.fetch("reviewedByStaff", JoinType.LEFT);

    List<Predicate> predicates = new ArrayList<>();
    predicates.add(criteriaBuilder.equal(root.get("tenant").get("id"), tenantId));
    predicates.add(criteriaBuilder.equal(root.get("parking").get("id"), parkingId));
    predicates.add(criteriaBuilder.equal(root.get("type"), PenaltyType.OCCUPIED_ASSIGNED_SLOT));
    predicates.add(criteriaBuilder.isTrue(root.get("reportedFromPwa")));

    if (status != null) {
      predicates.add(criteriaBuilder.equal(root.get("status"), status));
    }

    String normalizedSearch =
        reportedPlate == null || reportedPlate.isBlank()
            ? null
            : reportedPlate.trim().toLowerCase(Locale.ROOT);
    if (normalizedSearch != null) {
      predicates.add(
          criteriaBuilder.like(
              criteriaBuilder.lower(
                  criteriaBuilder.coalesce(root.<String>get("offenderLicensePlate"), "")),
              "%" + normalizedSearch + "%"));
    }

    if (fromDate != null) {
      predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
    }
    if (toDate != null) {
      predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate));
    }

    return entityManager
        .createQuery(
            query
                .select(root)
                .where(predicates.toArray(Predicate[]::new))
                .orderBy(criteriaBuilder.desc(root.get("createdAt"))))
        .getResultList();
  }
}
