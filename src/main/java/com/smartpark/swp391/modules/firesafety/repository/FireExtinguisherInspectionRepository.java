package com.smartpark.swp391.modules.firesafety.repository;

import com.smartpark.swp391.modules.firesafety.entity.FireExtinguisherInspection;
import com.smartpark.swp391.modules.firesafety.enumType.FireInspectionResult;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FireExtinguisherInspectionRepository
    extends JpaRepository<FireExtinguisherInspection, UUID> {

  @Query(
      """
          SELECT fi
          FROM FireExtinguisherInspection fi
          JOIN FETCH fi.fireExtinguisher fe
          JOIN FETCH fe.parking p
          JOIN FETCH fe.floor f
          LEFT JOIN FETCH fe.zone z
          LEFT JOIN FETCH fi.inspectedBy u
          WHERE fi.tenant.id = :tenantId
            AND (:extinguisherId IS NULL OR fe.id = :extinguisherId)
            AND (:parkingId IS NULL OR p.id = :parkingId)
            AND (:floorId IS NULL OR f.id = :floorId)
            AND (:result IS NULL OR fi.result = :result)
            AND (:from IS NULL OR fi.inspectedAt >= :from)
            AND (:to IS NULL OR fi.inspectedAt <= :to)
          """)
  Page<FireExtinguisherInspection> searchLogs(
      @Param("tenantId") UUID tenantId,
      @Param("extinguisherId") UUID extinguisherId,
      @Param("parkingId") UUID parkingId,
      @Param("floorId") UUID floorId,
      @Param("result") FireInspectionResult result,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      Pageable pageable);
}
