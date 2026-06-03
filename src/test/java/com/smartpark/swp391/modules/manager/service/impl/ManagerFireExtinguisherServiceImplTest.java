package com.smartpark.swp391.modules.manager.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedDownload;
import com.smartpark.swp391.infrastructure.storage.service.StorageService;
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
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherCoordinateRequest;
import com.smartpark.swp391.modules.manager.dto.firesafety.FireExtinguisherRequest;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.parking.repository.FloorRepository;
import com.smartpark.swp391.modules.parking.repository.ParkingRepository;
import com.smartpark.swp391.modules.parking.repository.ZoneRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ManagerFireExtinguisherServiceImplTest {

  @Mock FireExtinguisherRepository fireExtinguisherRepository;
  @Mock FireExtinguisherInspectionRepository inspectionRepository;
  @Mock ParkingRepository parkingRepository;
  @Mock FloorRepository floorRepository;
  @Mock ZoneRepository zoneRepository;
  @Mock TenantRepository tenantRepository;
  @Mock StorageService storageService;

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
  void listWithNullSearchUsesNoSearchQuery() {
    PageRequest pageable = PageRequest.of(0, 20, Sort.by("code").ascending());
    when(fireExtinguisherRepository.findByFilters(
            eq(data.tenant().getId()),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(pageable)))
        .thenReturn(pageOf(data.extinguisher(), pageable));

    var result = service().getExtinguishers(null, null, null, null, null, null, null, 0, 20);

    assertThat(result.content()).extracting("code").containsExactly("FE-B1-001");
    verify(fireExtinguisherRepository, never())
        .searchByText(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void listWithBlankSearchUsesNoSearchQuery() {
    PageRequest pageable = PageRequest.of(0, 20, Sort.by("code").ascending());
    when(fireExtinguisherRepository.findByFilters(
            eq(data.tenant().getId()),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(pageable)))
        .thenReturn(pageOf(data.extinguisher(), pageable));

    var result = service().getExtinguishers(null, null, null, null, null, "   ", null, 0, 20);

    assertThat(result.content()).extracting("code").containsExactly("FE-B1-001");
    verify(fireExtinguisherRepository, never())
        .searchByText(any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void listWithSearchUsesLowercaseSearchQuery() {
    PageRequest pageable = PageRequest.of(0, 20, Sort.by("code").ascending());
    when(fireExtinguisherRepository.searchByText(
            eq(data.tenant().getId()),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq("fe-b1"),
            isNull(),
            eq(pageable)))
        .thenReturn(pageOf(data.extinguisher(), pageable));

    var result = service().getExtinguishers(null, null, null, null, null, " FE-B1 ", null, 0, 20);

    assertThat(result.content()).extracting("code").containsExactly("FE-B1-001");
    verify(fireExtinguisherRepository, never())
        .findByFilters(any(), any(), any(), any(), any(), any(), any(), any(Pageable.class));
  }

  @Test
  void createRejectsCrossTenantParking() {
    when(parkingRepository.findByIdAndTenantIdAndIsDeletedFalse(
            data.parking().getId(), data.tenant().getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().createExtinguisher(request(data)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Parking not found");

    verify(fireExtinguisherRepository, never()).save(org.mockito.ArgumentMatchers.any());
  }

  @Test
  void updateCoordinateValidatesPercentRange() {
    when(fireExtinguisherRepository.findByIdAndTenantIdAndDeletedFalse(
            data.extinguisher().getId(), data.tenant().getId()))
        .thenReturn(Optional.of(data.extinguisher()));

    assertThatThrownBy(
            () ->
                service()
                    .updateCoordinate(
                        data.extinguisher().getId(),
                        new FireExtinguisherCoordinateRequest(
                            new BigDecimal("101.00"), BigDecimal.TEN)))
        .isInstanceOf(ApiException.class)
        .hasMessage("xCoordinate must be between 0 and 100");

    verify(fireExtinguisherRepository, never()).save(data.extinguisher());
  }

  @Test
  void summaryCountsStatusesAndDueDates() {
    when(fireExtinguisherRepository.countByStatus(data.tenant().getId()))
        .thenReturn(
            List.of(
                new Object[] {FireExtinguisherStatus.ACTIVE, 3L},
                new Object[] {FireExtinguisherStatus.EXPIRED, 1L},
                new Object[] {FireExtinguisherStatus.MAINTENANCE, 2L}));
    when(fireExtinguisherRepository.countByTenantIdAndDeletedFalse(data.tenant().getId()))
        .thenReturn(6L);
    when(fireExtinguisherRepository.countByTenantIdAndDeletedFalseAndNextInspectionAtLessThanEqual(
            org.mockito.ArgumentMatchers.eq(data.tenant().getId()),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(4L);
    when(fireExtinguisherRepository.countByTenantIdAndDeletedFalseAndExpiryDateBetween(
            org.mockito.ArgumentMatchers.eq(data.tenant().getId()),
            org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(2L);

    var summary = service().getSummary();

    assertThat(summary.total()).isEqualTo(6);
    assertThat(summary.active()).isEqualTo(3);
    assertThat(summary.expired()).isEqualTo(1);
    assertThat(summary.maintenance()).isEqualTo(2);
    assertThat(summary.dueInspection()).isEqualTo(4);
    assertThat(summary.expiringSoon()).isEqualTo(2);
  }

  @Test
  void fireSafetyMapReturnsExtinguishersForFloor() {
    when(floorRepository.findByIdAndTenantIdAndDeletedFalse(
            data.floor().getId(), data.tenant().getId()))
        .thenReturn(Optional.of(data.floor()));
    when(fireExtinguisherRepository.findMapItemsByTenantIdAndFloorId(
            data.tenant().getId(), data.floor().getId()))
        .thenReturn(List.of(data.extinguisher()));

    var map = service().getFireSafetyMap(data.floor().getId());

    assertThat(map.floorId()).isEqualTo(data.floor().getId());
    assertThat(map.mapImageUrl()).isEqualTo("tenants/demo/floor.png");
    assertThat(map.extinguishers()).hasSize(1);
    assertThat(map.extinguishers().getFirst().code()).isEqualTo("FE-B1-001");
    assertThat(map.extinguishers().getFirst().hasCoordinate()).isTrue();
  }

  @Test
  void inspectionLogsReturnPresignedPhotoDisplayUrlForObjectKey() {
    String objectKey = "tenants/" + data.tenant().getId() + "/fire-inspections/staff/photo.jpg";
    FireExtinguisherInspection inspection = inspection(data.extinguisher());
    inspection.setPhotoObjectKey(objectKey);
    PageRequest pageable = PageRequest.of(0, 20, Sort.by("inspectedAt").descending());
    when(inspectionRepository.searchLogs(
            eq(data.tenant().getId()),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(inspection), pageable, 1));
    when(storageService.createPresignedDownload(data.tenant().getId(), objectKey))
        .thenReturn(
            PresignedDownload.builder()
                .downloadUrl("https://storage/view")
                .expiresInSeconds(900)
                .build());

    var logs = service().getInspectionLogs(null, null, null, null, null, null, 0, 20);

    assertThat(logs.content()).hasSize(1);
    assertThat(logs.content().getFirst().photoObjectKey()).isEqualTo(objectKey);
    assertThat(logs.content().getFirst().photoDisplayUrl()).isEqualTo("https://storage/view");
    assertThat(logs.content().getFirst().photoUrlExpiresInSeconds()).isEqualTo(900);
  }

  @Test
  void inspectionLogsReturnLegacyHttpPhotoUrlAsDisplayUrl() {
    FireExtinguisherInspection inspection = inspection(data.extinguisher());
    inspection.setPhotoUrl("https://example.com/fire-inspections/demo.jpg");
    PageRequest pageable = PageRequest.of(0, 20, Sort.by("inspectedAt").descending());
    when(inspectionRepository.searchLogs(
            eq(data.tenant().getId()),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            isNull(),
            eq(pageable)))
        .thenReturn(new PageImpl<>(List.of(inspection), pageable, 1));

    var logs = service().getInspectionLogs(null, null, null, null, null, null, 0, 20);

    assertThat(logs.content()).hasSize(1);
    assertThat(logs.content().getFirst().photoUrl())
        .isEqualTo("https://example.com/fire-inspections/demo.jpg");
    assertThat(logs.content().getFirst().photoDisplayUrl())
        .isEqualTo("https://example.com/fire-inspections/demo.jpg");
    assertThat(logs.content().getFirst().photoUrlExpiresInSeconds()).isNull();
    verify(storageService, never()).createPresignedDownload(any(), any());
  }

  private ManagerFireExtinguisherServiceImpl service() {
    return new ManagerFireExtinguisherServiceImpl(
        fireExtinguisherRepository,
        inspectionRepository,
        parkingRepository,
        floorRepository,
        zoneRepository,
        tenantRepository,
        storageService);
  }

  private FireExtinguisherRequest request(TestData data) {
    return new FireExtinguisherRequest(
        data.parking().getId(),
        data.floor().getId(),
        data.zone().getId(),
        "FE-B1-001",
        FireExtinguisherType.CO2,
        "Near elevator",
        BigDecimal.TEN,
        BigDecimal.TEN,
        null,
        null,
        null,
        FireExtinguisherStatus.ACTIVE,
        null);
  }

  private PageImpl<FireExtinguisher> pageOf(FireExtinguisher extinguisher, PageRequest pageable) {
    return new PageImpl<>(List.of(extinguisher), pageable, 1);
  }

  private TestData testData() {
    Tenant tenant = Tenant.builder().name("Tenant").slug("tenant").emailContact("t@e.com").build();
    tenant.setId(UUID.randomUUID());
    Parking parking = Parking.builder().tenant(tenant).code("P").name("Parking").build();
    parking.setId(UUID.randomUUID());
    Floor floor =
        Floor.builder()
            .tenant(tenant)
            .parking(parking)
            .code("B1")
            .name("Basement 1")
            .mapImageUrl("tenants/demo/floor.png")
            .build();
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
            .xCoordinate(BigDecimal.TEN)
            .yCoordinate(BigDecimal.TEN)
            .build();
    extinguisher.setId(UUID.randomUUID());
    return new TestData(tenant, parking, floor, zone, extinguisher);
  }

  private FireExtinguisherInspection inspection(FireExtinguisher extinguisher) {
    FireExtinguisherInspection inspection =
        FireExtinguisherInspection.builder()
            .tenant(data.tenant())
            .fireExtinguisher(extinguisher)
            .result(FireInspectionResult.OK)
            .pressureOk(true)
            .sealOk(true)
            .locationOk(true)
            .expiryOk(true)
            .inspectedAt(java.time.LocalDateTime.now())
            .build();
    inspection.setId(UUID.randomUUID());
    return inspection;
  }

  private record TestData(
      Tenant tenant, Parking parking, Floor floor, Zone zone, FireExtinguisher extinguisher) {}
}
