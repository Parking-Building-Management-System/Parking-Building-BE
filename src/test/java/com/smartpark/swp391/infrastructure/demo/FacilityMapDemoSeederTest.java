package com.smartpark.swp391.infrastructure.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartpark.swp391.infrastructure.demo.config.DemoSeedProperties;
import com.smartpark.swp391.infrastructure.storage.config.MinioStorageProperties;
import com.smartpark.swp391.infrastructure.storage.service.StorageService;
import com.smartpark.swp391.modules.identity.entity.Tenant;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Parking;
import com.smartpark.swp391.modules.parking.repository.FloorRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;

@ExtendWith(MockitoExtension.class)
class FacilityMapDemoSeederTest {

  @Mock FloorRepository floorRepository;
  @Mock SlotRepository slotRepository;
  @Mock StorageService storageService;

  @Test
  void seedSkipsFloorMapsWithoutThrowingWhenStorageIsUnconfigured() {
    FacilityMapDemoSeeder seeder =
        seeder(new MinioStorageProperties("", "", "", "minio", "smartpark"));

    FacilityMapDemoSeedResult result = seeder.seed();

    assertThat(result.floorMapsSeeded()).isZero();
    assertThat(result.floorMapsSkippedMissingStorage()).isTrue();
    verify(storageService, never()).uploadObject(any(), any(), any(), anyLong(), any());
  }

  @Test
  void seedUploadsAndSetsMapImageUrlOnlyAfterUploadSucceeds() {
    Floor floor = floor(null);
    when(floorRepository.findAllForDemoSeed()).thenReturn(List.of(floor));
    when(storageService.objectExists(eq(floor.getTenant().getId()), any())).thenReturn(false);

    FacilityMapDemoSeedResult result = seeder(configuredStorage()).seed();

    assertThat(result.floorMapsSeeded()).isEqualTo(1);
    assertThat(floor.getMapImageUrl())
        .startsWith("tenants/" + floor.getTenant().getId() + "/floor-maps/demo/");
    verify(storageService)
        .uploadObject(
            eq(floor.getTenant().getId()),
            eq(floor.getMapImageUrl()),
            any(InputStream.class),
            anyLong(),
            any());
    verify(floorRepository).saveAll(List.of(floor));
  }

  @Test
  void seedReuploadsExistingObjectKeyWhenObjectIsMissing() {
    String objectKey = "tenants/" + UUID.randomUUID() + "/floor-maps/demo/missing.png";
    Floor floor = floor(objectKey);
    objectKey = "tenants/" + floor.getTenant().getId() + "/floor-maps/demo/missing.png";
    floor.setMapImageUrl(objectKey);
    when(floorRepository.findAllForDemoSeed()).thenReturn(List.of(floor));
    when(storageService.objectExists(floor.getTenant().getId(), objectKey)).thenReturn(false);

    FacilityMapDemoSeedResult result = seeder(configuredStorage()).seed();

    assertThat(result.floorMapsSeeded()).isEqualTo(1);
    assertThat(floor.getMapImageUrl()).isEqualTo(objectKey);
    verify(storageService)
        .uploadObject(
            eq(floor.getTenant().getId()), eq(objectKey), any(InputStream.class), anyLong(), any());
  }

  @Test
  void seedSkipsExistingObjectWithoutDuplicateUpload() {
    Floor floor = floor(null);
    String objectKey = "tenants/" + floor.getTenant().getId() + "/floor-maps/demo/existing.png";
    floor.setMapImageUrl(objectKey);
    when(floorRepository.findAllForDemoSeed()).thenReturn(List.of(floor));
    when(storageService.objectExists(floor.getTenant().getId(), objectKey)).thenReturn(true);

    FacilityMapDemoSeedResult result = seeder(configuredStorage()).seed();

    assertThat(result.floorMapsSeeded()).isZero();
    assertThat(result.floorMapsSkippedConfigured()).isEqualTo(1);
    verify(storageService, never()).uploadObject(any(), any(), any(), anyLong(), any());
  }

  @Test
  void seedDoesNotWriteObjectKeyWhenUploadFails() {
    Floor floor = floor(null);
    when(floorRepository.findAllForDemoSeed()).thenReturn(List.of(floor));
    when(storageService.objectExists(eq(floor.getTenant().getId()), any())).thenReturn(false);
    doThrow(new RuntimeException("MinIO unavailable"))
        .when(storageService)
        .uploadObject(eq(floor.getTenant().getId()), any(), any(), anyLong(), any());

    FacilityMapDemoSeedResult result = seeder(configuredStorage()).seed();

    assertThat(result.floorMapsSeeded()).isZero();
    assertThat(result.floorMapsSkippedMissingStorage()).isTrue();
    assertThat(floor.getMapImageUrl()).isNull();
  }

  private FacilityMapDemoSeeder seeder(MinioStorageProperties storageProperties) {
    return new FacilityMapDemoSeeder(
        new DemoSeedProperties(true, true, false),
        storageProperties,
        storageService,
        floorRepository,
        slotRepository,
        new DefaultResourceLoader());
  }

  private MinioStorageProperties configuredStorage() {
    return new MinioStorageProperties(
        "http://localhost:9000", "minioadmin", "minioadmin", "minio", "smartpark");
  }

  private Floor floor(String mapImageUrl) {
    UUID tenantId = UUID.randomUUID();
    UUID parkingId = UUID.randomUUID();
    Tenant tenant =
        Tenant.builder().name("Tenant").slug("tenant").emailContact("t@example.com").build();
    tenant.setId(tenantId);
    Parking parking = Parking.builder().tenant(tenant).code("P1").name("Parking").build();
    parking.setId(parkingId);
    Floor floor =
        Floor.builder()
            .tenant(tenant)
            .parking(parking)
            .code("B1")
            .name("Basement 1")
            .mapImageUrl(mapImageUrl)
            .build();
    floor.setId(UUID.randomUUID());
    return floor;
  }
}
