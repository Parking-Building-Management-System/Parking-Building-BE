package com.smartpark.swp391.modules.penalty.repository;

import com.smartpark.swp391.modules.penalty.entity.PenaltyCase;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PenaltyCaseRepositoryCustom {

  List<PenaltyCase> findViolationReports(
      UUID tenantId,
      UUID parkingId,
      PenaltyCaseStatus status,
      String reportedPlate,
      LocalDateTime fromDate,
      LocalDateTime toDate);
}
