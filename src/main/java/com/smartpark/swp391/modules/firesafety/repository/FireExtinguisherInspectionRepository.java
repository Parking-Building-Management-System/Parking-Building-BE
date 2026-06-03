package com.smartpark.swp391.modules.firesafety.repository;

import com.smartpark.swp391.modules.firesafety.entity.FireExtinguisherInspection;
import com.smartpark.swp391.modules.firesafety.enumType.FireInspectionResult;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FireExtinguisherInspectionRepository
    extends JpaRepository<FireExtinguisherInspection, UUID>,
        JpaSpecificationExecutor<FireExtinguisherInspection> {

  @Override
  @EntityGraph(
      attributePaths = {
        "fireExtinguisher",
        "fireExtinguisher.parking",
        "fireExtinguisher.floor",
        "fireExtinguisher.zone",
        "inspectedBy"
      })
  Page<FireExtinguisherInspection> findAll(
      Specification<FireExtinguisherInspection> specification, Pageable pageable);

  default Page<FireExtinguisherInspection> searchLogs(
      UUID tenantId,
      UUID extinguisherId,
      UUID parkingId,
      UUID floorId,
      FireInspectionResult result,
      LocalDateTime from,
      LocalDateTime to,
      Pageable pageable) {
    return findAll(
        logsSpecification(tenantId, extinguisherId, parkingId, floorId, result, from, to),
        pageable);
  }

  private static Specification<FireExtinguisherInspection> logsSpecification(
      UUID tenantId,
      UUID extinguisherId,
      UUID parkingId,
      UUID floorId,
      FireInspectionResult result,
      LocalDateTime from,
      LocalDateTime to) {
    return (root, query, criteriaBuilder) -> {
      var predicates = new ArrayList<Predicate>();
      var extinguisher = root.join("fireExtinguisher");

      predicates.add(criteriaBuilder.equal(root.get("tenant").get("id"), tenantId));
      if (extinguisherId != null) {
        predicates.add(criteriaBuilder.equal(extinguisher.get("id"), extinguisherId));
      }
      if (parkingId != null) {
        predicates.add(criteriaBuilder.equal(extinguisher.join("parking").get("id"), parkingId));
      }
      if (floorId != null) {
        predicates.add(criteriaBuilder.equal(extinguisher.join("floor").get("id"), floorId));
      }
      if (result != null) {
        predicates.add(criteriaBuilder.equal(root.get("result"), result));
      }
      if (from != null) {
        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("inspectedAt"), from));
      }
      if (to != null) {
        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("inspectedAt"), to));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
