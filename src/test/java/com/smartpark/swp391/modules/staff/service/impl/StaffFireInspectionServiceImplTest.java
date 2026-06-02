package com.smartpark.swp391.modules.staff.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.tenant.TenantContext;
import com.smartpark.swp391.modules.firesafety.entity.FireExtinguisher;
import com.smartpark.swp391.modules.firesafety.entity.FireExtinguisherInspection;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherStatus;
import com.smartpark.swp391.modules.firesafety.enumType.FireExtinguisherType;
import com.smartpark.swp391.modules.firesafety.enumType.FireInspectionResult;
import com.smartpark.swp391.modules.firesafety.repository.FireExtinguisherInspectionRepository;
import com.smartpark.swp391.modules.firesafety.repository.FireExtinguisherRepository;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.identity.repository.TenantRepository;
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.staff.dto.StaffWorkContextResponse;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffFireInspectionRequest;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StaffFireInspectionServiceImplTest {

  @Mock FireExtinguisherRepository fireExtinguisherRepository;
  @Mock FireExtinguisherInspectionRepository inspectionRepository;
  @Mock TenantRepository tenantRepository;
  @Mock UserRepository userRepository;
  @Mock StaffWorkContextService staffWorkContextService;

  TestData data;

  @BeforeEach
  void setUp() {
    data = testData();
    TenantContext.setTenantId(data.tenant().getId());
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
  }

  @Test
  void dueInspectionsAreScopedToKioskParking() {
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(workContext(data.parking().getId()));
    when(fireExtinguisherRepository.findDueForStaffParking(
            org.mockito.ArgumentMatchers.eq(data.tenant().getId()),
            org.mockito.ArgumentMatchers.eq(data.parking().getId()),
            org.mockito.ArgumentMatchers.isNull(),
            org.mockito.ArgumentMatchers.eq(FireExtinguisherStatus.ACTIVE),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(List.of(data.extinguisher()));

    var due = service().getDueInspections(null, FireExtinguisherStatus.ACTIVE);

    assertThat(due).hasSize(1);
    assertThat(due.getFirst().fireExtinguisherId()).isEqualTo(data.extinguisher().getId());
    assertThat(due.getFirst().parkingName()).isEqualTo(data.parking().getName());
  }

  @Test
  void submitInspectionUpdatesExtinguisherStatus() {
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(workContext(data.parking().getId()));
    when(fireExtinguisherRepository.findByIdAndTenantIdAndDeletedFalse(
            data.extinguisher().getId(), data.tenant().getId()))
        .thenReturn(Optional.of(data.extinguisher()));
    when(tenantRepository.getReferenceById(data.tenant().getId())).thenReturn(data.tenant());
    when(inspectionRepository.save(org.mockito.ArgumentMatchers.any()))
        .thenAnswer(
            invocation -> {
              FireExtinguisherInspection inspection = invocation.getArgument(0);
              inspection.setId(UUID.randomUUID());
              return inspection;
            });

    var response =
        service()
            .submitInspection(
                new StaffFireInspectionRequest(
                    data.extinguisher().getId(),
                    FireInspectionResult.NEEDS_REPLACEMENT,
                    false,
                    true,
                    true,
                    true,
                    null,
                    "Low pressure",
                    LocalDateTime.now().plusDays(30)));

    assertThat(response.status()).isEqualTo(FireExtinguisherStatus.MAINTENANCE);
    assertThat(data.extinguisher().getStatus()).isEqualTo(FireExtinguisherStatus.MAINTENANCE);
    assertThat(data.extinguisher().getLastInspectedAt()).isNotNull();
  }

  @Test
  void submitInspectionRejectsOtherParking() {
    UUID otherParkingId = UUID.randomUUID();
    when(staffWorkContextService.requireCurrentContext()).thenReturn(workContext(otherParkingId));
    when(fireExtinguisherRepository.findByIdAndTenantIdAndDeletedFalse(
            data.extinguisher().getId(), data.tenant().getId()))
        .thenReturn(Optional.of(data.extinguisher()));

    assertThatThrownBy(
            () ->
                service()
                    .submitInspection(
                        new StaffFireInspectionRequest(
                            data.extinguisher().getId(),
                            FireInspectionResult.OK,
                            true,
                            true,
                            true,
                            true,
                            null,
                            null,
                            null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("EXTINGUISHER_NOT_IN_KIOSK_PARKING");
  }

  private StaffFireInspectionServiceImpl service() {
    return new StaffFireInspectionServiceImpl(
        fireExtinguisherRepository,
        inspectionRepository,
        tenantRepository,
        userRepository,
        staffWorkContextService);
  }

  private StaffWorkContextResponse workContext(UUID parkingId) {
    return StaffWorkContextResponse.builder()
        .kioskId(UUID.randomUUID())
        .kioskName("Entry")
        .kioskType(KioskType.MIXED)
        .parkingId(parkingId)
        .parkingName("Parking")
        .build();
  }

  private TestData testData() {
    Tenant tenant = Tenant.builder().name("Tenant").slug("tenant").emailContact("t@e.com").build();
    tenant.setId(UUID.randomUUID());
    Parking parking = Parking.builder().tenant(tenant).code("P").name("Parking").build();
    parking.setId(UUID.randomUUID());
    Floor floor = Floor.builder().tenant(tenant).parking(parking).code("B1").name("B1").build();
    floor.setId(UUID.randomUUID());
    Zone zone =
        Zone.builder().tenant(tenant).parking(parking).floor(floor).code("A").name("A").build();
    zone.setId(UUID.randomUUID());
    FireExtinguisher extinguisher =
        FireExtinguisher.builder()
            .tenant(tenant)
            .parking(parking)
            .floor(floor)
            .zone(zone)
            .code("FE-B1-001")
            .type(FireExtinguisherType.CO2)
            .status(FireExtinguisherStatus.ACTIVE)
            .expiryDate(LocalDate.now().plusYears(1))
            .build();
    extinguisher.setId(UUID.randomUUID());
    return new TestData(tenant, parking, floor, zone, extinguisher);
  }

  private record TestData(
      Tenant tenant, Parking parking, Floor floor, Zone zone, FireExtinguisher extinguisher) {}
}
