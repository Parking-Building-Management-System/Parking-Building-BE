package com.smartpark.swp391.modules.staff.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.infrastructure.storage.service.StorageService;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.entity.User;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.operation.entity.ParkingSession;
import com.smartpark.swp391.modules.operation.repository.ParkingSessionRepository;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.penalty.entity.PenaltyCase;
import com.smartpark.swp391.modules.penalty.entity.PenaltyRule;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyCaseStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyRuleStatus;
import com.smartpark.swp391.modules.penalty.enumType.PenaltyType;
import com.smartpark.swp391.modules.penalty.repository.PenaltyCaseRepository;
import com.smartpark.swp391.modules.penalty.service.PenaltyRuleLookupService;
import com.smartpark.swp391.modules.staff.dto.StaffResolvedContext;
import com.smartpark.swp391.modules.staff.dto.violation.ViolationReportApproveRequest;
import com.smartpark.swp391.modules.staff.dto.violation.ViolationReportRejectRequest;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffViolationReportServiceImplTest {

  @Mock PenaltyCaseRepository penaltyCaseRepository;
  @Mock ParkingSessionRepository parkingSessionRepository;
  @Mock PenaltyRuleLookupService penaltyRuleLookupService;
  @Mock StaffWorkContextService staffWorkContextService;
  @Mock UserRepository userRepository;
  @Mock StorageService storageService;

  @AfterEach
  void clearTenantContext() {
    TenantContext.clear();
  }

  @Test
  void pendingCountUsesCurrentTenantAndParkingContext() {
    TestData data = testData();
    when(staffWorkContextService.requireCurrentResolvedContext()).thenReturn(data.context);
    when(penaltyCaseRepository.countPendingViolationReports(
            data.tenant.getId(), data.parking.getId()))
        .thenReturn(3L);

    var response = service().getPendingCount();

    assertThat(response.pendingCount()).isEqualTo(3);
  }

  @Test
  void approveChangesPendingReportToAppliedUsingCurrentRuleAmount() {
    TestData data = testData();
    TenantContext.setTenantId(data.tenant.getId());
    when(staffWorkContextService.requireCurrentResolvedContext()).thenReturn(data.context);
    when(penaltyCaseRepository.findViolationReportForUpdate(
            data.tenant.getId(), data.parking.getId(), data.report.getId()))
        .thenReturn(Optional.of(data.report));
    when(penaltyRuleLookupService.requireActiveRule(
            data.tenant.getId(), data.parking.getId(), PenaltyType.OCCUPIED_ASSIGNED_SLOT))
        .thenReturn(data.rule);
    when(userRepository.getReferenceById(data.context.staffId())).thenReturn(data.reviewer);
    when(penaltyCaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        service()
            .approve(
                data.report.getId(),
                new ViolationReportApproveRequest("51A 99999", "Verified at exit lane."));

    assertThat(data.report.getStatus()).isEqualTo(PenaltyCaseStatus.APPLIED);
    assertThat(data.report.getAmount()).isEqualByComparingTo("50000");
    assertThat(data.report.getTargetSession()).isEqualTo(data.offender);
    assertThat(response.appliedAmount()).isEqualByComparingTo("50000");
  }

  @Test
  void rejectChangesPendingReportToRejectedWithoutCharge() {
    TestData data = testData();
    TenantContext.setTenantId(data.tenant.getId());
    when(staffWorkContextService.requireCurrentResolvedContext()).thenReturn(data.context);
    when(penaltyCaseRepository.findViolationReportForUpdate(
            data.tenant.getId(), data.parking.getId(), data.report.getId()))
        .thenReturn(Optional.of(data.report));
    when(userRepository.getReferenceById(data.context.staffId())).thenReturn(data.reviewer);
    when(penaltyCaseRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        service()
            .reject(
                data.report.getId(),
                new ViolationReportRejectRequest("No violating vehicle found."));

    assertThat(data.report.getStatus()).isEqualTo(PenaltyCaseStatus.REJECTED);
    assertThat(data.report.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(response.appliedAmount()).isNull();
  }

  @Test
  void staffCannotReviewAReportOutsideCurrentParking() {
    TestData data = testData();
    TenantContext.setTenantId(data.tenant.getId());
    when(staffWorkContextService.requireCurrentResolvedContext()).thenReturn(data.context);
    when(penaltyCaseRepository.findViolationReportForUpdate(
            data.tenant.getId(), data.parking.getId(), data.report.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service()
                    .approve(
                        data.report.getId(),
                        new ViolationReportApproveRequest("51A-99999", "Cross-parking attempt")))
        .isInstanceOf(com.smartpark.swp391.common.exception.ApiException.class)
        .hasMessage("VIOLATION_REPORT_NOT_FOUND");

    verify(penaltyRuleLookupService, never()).requireActiveRule(any(), any(), any());
  }

  @Test
  void duplicateReviewIsRejected() {
    TestData data = testData();
    data.report.setStatus(PenaltyCaseStatus.REJECTED);
    TenantContext.setTenantId(data.tenant.getId());
    when(staffWorkContextService.requireCurrentResolvedContext()).thenReturn(data.context);
    when(penaltyCaseRepository.findViolationReportForUpdate(
            data.tenant.getId(), data.parking.getId(), data.report.getId()))
        .thenReturn(Optional.of(data.report));

    assertThatThrownBy(
            () ->
                service()
                    .reject(
                        data.report.getId(), new ViolationReportRejectRequest("Already reviewed.")))
        .isInstanceOf(com.smartpark.swp391.common.exception.ApiException.class)
        .hasMessage("VIOLATION_REPORT_ALREADY_REVIEWED");

    verify(penaltyCaseRepository, never()).save(any());
  }

  private StaffViolationReportServiceImpl service() {
    return new StaffViolationReportServiceImpl(
        penaltyCaseRepository,
        parkingSessionRepository,
        penaltyRuleLookupService,
        staffWorkContextService,
        userRepository,
        storageService);
  }

  private TestData testData() {
    Tenant tenant =
        Tenant.builder().name("Tenant").slug("tenant").emailContact("tenant@example.com").build();
    tenant.setId(UUID.randomUUID());
    Parking parking = Parking.builder().tenant(tenant).code("P1").name("Parking 1").build();
    parking.setId(UUID.randomUUID());
    LocalDateTime reportedAt = LocalDateTime.now().minusMinutes(5);
    ParkingSession offender =
        ParkingSession.builder()
            .tenant(tenant)
            .parking(parking)
            .licensePlate("51A-99999")
            .checkInAt(reportedAt.minusMinutes(10))
            .build();
    offender.setId(UUID.randomUUID());
    ParkingSession victim =
        ParkingSession.builder()
            .tenant(tenant)
            .parking(parking)
            .licensePlate("51A-00001")
            .checkInAt(reportedAt.minusMinutes(15))
            .build();
    victim.setId(UUID.randomUUID());
    PenaltyCase report =
        PenaltyCase.builder()
            .tenant(tenant)
            .parking(parking)
            .type(PenaltyType.OCCUPIED_ASSIGNED_SLOT)
            .amount(BigDecimal.ZERO)
            .currency("VND")
            .status(PenaltyCaseStatus.REPORTED)
            .victimSession(victim)
            .offenderSession(offender)
            .targetSession(offender)
            .offenderLicensePlate("51A-99999")
            .reportedFromPwa(true)
            .build();
    report.setId(UUID.randomUUID());
    report.setCreatedAt(reportedAt);
    PenaltyRule rule =
        PenaltyRule.builder()
            .tenant(tenant)
            .parking(parking)
            .code("OCCUPIED")
            .name("Occupied assigned slot")
            .type(PenaltyType.OCCUPIED_ASSIGNED_SLOT)
            .amount(new BigDecimal("50000"))
            .currency("VND")
            .status(PenaltyRuleStatus.ACTIVE)
            .build();
    User reviewer = User.builder().tenant(tenant).username("staff").fullName("Staff").build();
    reviewer.setId(UUID.randomUUID());
    StaffResolvedContext context =
        StaffResolvedContext.builder()
            .tenantId(tenant.getId())
            .staffId(reviewer.getId())
            .parkingId(parking.getId())
            .kioskId(UUID.randomUUID())
            .build();
    return new TestData(tenant, parking, report, offender, rule, reviewer, context);
  }

  private record TestData(
      Tenant tenant,
      Parking parking,
      PenaltyCase report,
      ParkingSession offender,
      PenaltyRule rule,
      User reviewer,
      StaffResolvedContext context) {}
}
