package com.smartpark.swp391.modules.staff.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.common.exception.ApiException;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedDownload;
import com.smartpark.swp391.infrastructure.storage.dto.PresignedUpload;
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
import com.smartpark.swp391.modules.identity.repository.UserRepository;
import com.smartpark.swp391.modules.operation.enumType.KioskType;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.staff.dto.StaffWorkContextResponse;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffFireInspectionRequest;
import com.smartpark.swp391.modules.staff.dto.firesafety.StaffInspectionPhotoPresignRequest;
import com.smartpark.swp391.modules.staff.service.StaffWorkContextService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class StaffFireInspectionServiceImplTest {

  @Mock FireExtinguisherRepository fireExtinguisherRepository;
  @Mock FireExtinguisherInspectionRepository inspectionRepository;
  @Mock TenantRepository tenantRepository;
  @Mock UserRepository userRepository;
  @Mock StaffWorkContextService staffWorkContextService;
  @Mock StorageService storageService;

  TestData data;
  UUID staffId;

  @BeforeEach
  void setUp() {
    data = testData();
    staffId = UUID.randomUUID();
    TenantContext.setTenantId(data.tenant().getId());
    SecurityContextHolder.getContext().setAuthentication(jwtAuthentication(staffId));
  }

  @AfterEach
  void tearDown() {
    TenantContext.clear();
    SecurityContextHolder.clearContext();
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
  void createPhotoUploadUsesStaffTenantAndFixedInspectionFolder() {
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(workContext(data.parking().getId()));
    when(storageService.createPresignedUpload(
            data.tenant().getId(), "fire-inspections/" + staffId, "FE-B1-001.jpg", "image/jpeg"))
        .thenReturn(
            PresignedUpload.builder()
                .objectKey(
                    "tenants/"
                        + data.tenant().getId()
                        + "/fire-inspections/"
                        + staffId
                        + "/photo.jpg")
                .uploadUrl("https://storage/upload")
                .method("PUT")
                .headers(Map.of("Content-Type", "image/jpeg"))
                .expiresInSeconds(900)
                .build());

    var response =
        service()
            .createPhotoUpload(
                new StaffInspectionPhotoPresignRequest("FE-B1-001.jpg", "image/jpeg"));

    assertThat(response.uploadUrl()).isEqualTo("https://storage/upload");
    assertThat(response.method()).isEqualTo("PUT");
    assertThat(response.objectKey())
        .startsWith("tenants/" + data.tenant().getId() + "/fire-inspections/" + staffId + "/");
  }

  @Test
  void createPhotoUploadRejectsNonImageContentType() {
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(workContext(data.parking().getId()));

    assertThatThrownBy(
            () ->
                service()
                    .createPhotoUpload(
                        new StaffInspectionPhotoPresignRequest(
                            "inspection.pdf", "application/pdf")))
        .isInstanceOf(ApiException.class)
        .hasMessage("contentType must be image/jpeg, image/png, or image/webp");

    verify(storageService, never()).createPresignedUpload(any(), any(), any(), any());
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
                    null,
                    "Low pressure",
                    LocalDateTime.now().plusDays(30)));

    assertThat(response.status()).isEqualTo(FireExtinguisherStatus.MAINTENANCE);
    assertThat(data.extinguisher().getStatus()).isEqualTo(FireExtinguisherStatus.MAINTENANCE);
    assertThat(data.extinguisher().getLastInspectedAt()).isNotNull();
  }

  @Test
  void submitInspectionStoresPhotoObjectKeyUnderTenantInspectionPrefix() {
    String objectKey =
        "tenants/" + data.tenant().getId() + "/fire-inspections/" + staffId + "/photo.jpg";
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(workContext(data.parking().getId()));
    when(fireExtinguisherRepository.findByIdAndTenantIdAndDeletedFalse(
            data.extinguisher().getId(), data.tenant().getId()))
        .thenReturn(Optional.of(data.extinguisher()));
    when(tenantRepository.getReferenceById(data.tenant().getId())).thenReturn(data.tenant());
    when(storageService.createPresignedDownload(data.tenant().getId(), objectKey))
        .thenReturn(
            PresignedDownload.builder()
                .downloadUrl("https://storage/view")
                .expiresInSeconds(900)
                .build());
    when(inspectionRepository.save(any()))
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
                    FireInspectionResult.OK,
                    true,
                    true,
                    true,
                    true,
                    null,
                    objectKey,
                    null,
                    LocalDateTime.now().plusDays(30)));

    assertThat(response.photoObjectKey()).isEqualTo(objectKey);
    assertThat(response.photoDisplayUrl()).isEqualTo("https://storage/view");
    assertThat(response.photoUrlExpiresInSeconds()).isEqualTo(900);
  }

  @Test
  void submitInspectionRejectsPhotoObjectKeyOutsideTenantInspectionPrefix() {
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(workContext(data.parking().getId()));
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
                            "tenants/" + UUID.randomUUID() + "/fire-inspections/photo.jpg",
                            null,
                            null)))
        .isInstanceOf(ApiException.class)
        .hasMessage("Photo object key is outside current tenant fire inspections");

    verify(inspectionRepository, never()).save(any());
  }

  @Test
  void submitInspectionKeepsLegacyHttpPhotoUrlDisplay() {
    when(staffWorkContextService.requireCurrentContext())
        .thenReturn(workContext(data.parking().getId()));
    when(fireExtinguisherRepository.findByIdAndTenantIdAndDeletedFalse(
            data.extinguisher().getId(), data.tenant().getId()))
        .thenReturn(Optional.of(data.extinguisher()));
    when(tenantRepository.getReferenceById(data.tenant().getId())).thenReturn(data.tenant());
    when(inspectionRepository.save(any()))
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
                    FireInspectionResult.OK,
                    true,
                    true,
                    true,
                    true,
                    "https://example.com/fire-inspections/demo.jpg",
                    null,
                    null,
                    null));

    assertThat(response.photoDisplayUrl())
        .isEqualTo("https://example.com/fire-inspections/demo.jpg");
    assertThat(response.photoUrlExpiresInSeconds()).isNull();
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
        staffWorkContextService,
        storageService);
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

  private JwtAuthenticationToken jwtAuthentication(UUID userId) {
    Jwt jwt =
        Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .claim("user_id", userId.toString())
            .claim("tenant_id", data.tenant().getId().toString())
            .build();
    return new JwtAuthenticationToken(jwt);
  }

  private record TestData(
      Tenant tenant, Parking parking, Floor floor, Zone zone, FireExtinguisher extinguisher) {}
}
