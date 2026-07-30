package com.smartpark.swp391.modules.staff.service;

import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import com.smartpark.swp391.modules.staff.dto.violation.PendingViolationReportCountResponse;
import com.smartpark.swp391.modules.staff.dto.violation.ViolationReportApproveRequest;
import com.smartpark.swp391.modules.staff.dto.violation.ViolationReportRejectRequest;
import com.smartpark.swp391.modules.staff.dto.violation.ViolationReportResponse;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface StaffViolationReportService {
  PendingViolationReportCountResponse getPendingCount();

  List<ViolationReportResponse> getReports(
      PenaltyCaseStatus status, String reportedPlate, LocalDateTime from, LocalDateTime to);

  ViolationReportResponse getReport(UUID id);

  ViolationReportResponse approve(UUID id, ViolationReportApproveRequest request);

  ViolationReportResponse reject(UUID id, ViolationReportRejectRequest request);
}
