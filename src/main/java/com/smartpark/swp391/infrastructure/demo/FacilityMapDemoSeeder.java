package com.smartpark.swp391.infrastructure.demo;

import com.smartpark.swp391.infrastructure.demo.config.DemoSeedProperties;
import com.smartpark.swp391.infrastructure.storage.config.MinioStorageProperties;
import com.smartpark.swp391.infrastructure.storage.service.StorageService;
import com.smartpark.swp391.modules.parking.entity.Floor;
import com.smartpark.swp391.modules.parking.entity.Slot;
import com.smartpark.swp391.modules.parking.entity.Zone;
import com.smartpark.swp391.modules.parking.repository.FloorRepository;
import com.smartpark.swp391.modules.parking.repository.SlotRepository;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class FacilityMapDemoSeeder {

  private static final String ASSET_BASE = "classpath:demo-assets/floor-maps/";
  private static final List<DemoMapAsset> DEMO_MAP_ASSETS =
      List.of(
          new DemoMapAsset("demo-parking-1.png", "image/png"),
          new DemoMapAsset("demo-parking-2.png", "image/png"),
          new DemoMapAsset("demo-parking-3.png", "image/png"),
          new DemoMapAsset("demo-parking-4.jpg", "image/jpeg"),
          new DemoMapAsset("demo-parking-5.jpg", "image/jpeg"));
  private static final Pattern NATURAL_TOKEN = Pattern.compile("\\d+|\\D+");

  DemoSeedProperties properties;
  MinioStorageProperties minioStorageProperties;
  StorageService storageService;
  FloorRepository floorRepository;
  SlotRepository slotRepository;
  ResourceLoader resourceLoader;

  @Transactional
  public FacilityMapDemoSeedResult seed() {
    SeedStats stats = new SeedStats();

    if (properties.floorMapsEnabled()) {
      seedFloorMaps(stats);
    }
    if (properties.slotCoordinatesEnabled()) {
      seedSlotCoordinates(stats);
    }

    return FacilityMapDemoSeedResult.builder()
        .floorMapsSeeded(stats.floorMapsSeeded)
        .floorMapsSkippedConfigured(stats.floorMapsSkippedConfigured)
        .slotCoordinatesSeeded(stats.slotCoordinatesSeeded)
        .slotCoordinatesSkippedConfigured(stats.slotCoordinatesSkippedConfigured)
        .floorMapsSkippedMissingStorage(stats.floorMapsSkippedMissingStorage)
        .build();
  }

  private void seedFloorMaps(SeedStats stats) {
    List<Floor> floors = floorRepository.findAllForDemoSeed();

    if (!minioStorageProperties.configured()) {
      stats.floorMapsSkippedMissingStorage = true;
      log.warn("Demo floor map seed skipped: storage not configured.");
      return;
    }

    for (Floor floor : floors) {
      String currentMapImageUrl = floor.getMapImageUrl();
      if (isExternalUrl(currentMapImageUrl)) {
        stats.floorMapsSkippedConfigured++;
        continue;
      }

      DemoMapAsset asset = assetFor(floor.getId());
      String objectKey =
          hasText(currentMapImageUrl)
              ? currentMapImageUrl.trim()
              : demoObjectKey(floor, asset.fileName());
      if (objectExists(floor.getTenant().getId(), objectKey, stats)) {
        stats.floorMapsSkippedConfigured++;
        continue;
      }

      if (!uploadDemoAsset(floor.getTenant().getId(), objectKey, asset, stats)) {
        continue;
      }

      if (!hasText(currentMapImageUrl)) {
        floor.setMapImageUrl(objectKey);
      }
      stats.floorMapsSeeded++;
    }

    floorRepository.saveAll(floors);
  }

  private boolean objectExists(UUID tenantId, String objectKey, SeedStats stats) {
    try {
      return storageService.objectExists(tenantId, objectKey);
    } catch (RuntimeException e) {
      log.warn("Demo floor map seed could not verify object '{}': {}", objectKey, e.getMessage());
      stats.floorMapsSkippedMissingStorage = true;
      return false;
    }
  }

  private boolean uploadDemoAsset(
      UUID tenantId, String objectKey, DemoMapAsset asset, SeedStats stats) {
    Resource resource = resourceLoader.getResource(ASSET_BASE + asset.fileName());
    if (!resource.exists()) {
      log.warn(
          "Skipping facility map demo image seed because asset is missing: {}", asset.fileName());
      stats.floorMapsSkippedMissingStorage = true;
      return false;
    }

    try (InputStream inputStream = resource.getInputStream()) {
      storageService.uploadObject(
          tenantId, objectKey, inputStream, resource.contentLength(), asset.contentType());
      return true;
    } catch (IOException | RuntimeException e) {
      log.warn(
          "Demo floor map seed skipped object '{}' after upload failure: {}",
          objectKey,
          e.getMessage());
      stats.floorMapsSkippedMissingStorage = true;
      return false;
    }
  }

  private DemoMapAsset assetFor(UUID floorId) {
    int index = Math.floorMod(floorId.hashCode(), DEMO_MAP_ASSETS.size());
    return DEMO_MAP_ASSETS.get(index);
  }

  private String demoObjectKey(Floor floor, String assetFileName) {
    return "tenants/"
        + floor.getTenant().getId()
        + "/floor-maps/demo/"
        + floor.getId()
        + "-"
        + assetFileName;
  }

  private void seedSlotCoordinates(SeedStats stats) {
    List<Slot> slots = slotRepository.findAllForDemoCoordinateSeed();
    stats.slotCoordinatesSkippedConfigured =
        (int) slots.stream().filter(this::hasCompleteCoordinate).count();

    Map<UUID, List<Slot>> slotsByFloor =
        slots.stream()
            .filter(slot -> !hasCompleteCoordinate(slot))
            .filter(slot -> floorId(slot) != null)
            .collect(Collectors.groupingBy(this::floorId));

    for (List<Slot> floorSlots : slotsByFloor.values()) {
      seedFloorSlotCoordinates(floorSlots, stats);
    }

    slotRepository.saveAll(slots);
  }

  private void seedFloorSlotCoordinates(List<Slot> floorSlots, SeedStats stats) {
    List<Zone> zones =
        floorSlots.stream().map(Slot::getZone).distinct().sorted(zoneComparator()).toList();

    for (int zoneIndex = 0; zoneIndex < zones.size(); zoneIndex++) {
      Zone zone = zones.get(zoneIndex);
      List<Slot> zoneSlots =
          floorSlots.stream()
              .filter(slot -> zone.getId().equals(slot.getZone().getId()))
              .sorted(slotComparator())
              .toList();
      seedZoneSlotCoordinates(zoneSlots, zoneIndex, stats);
    }
  }

  private void seedZoneSlotCoordinates(List<Slot> slots, int zoneIndex, SeedStats stats) {
    if (slots.isEmpty()) {
      return;
    }

    Band band = band(zoneIndex);
    int slotCount = slots.size();
    int columns = Math.min(12, Math.max(4, (int) Math.ceil(Math.sqrt(slotCount * 1.6D))));
    int rows = (int) Math.ceil((double) slotCount / columns);

    for (int index = 0; index < slotCount; index++) {
      Slot slot = slots.get(index);
      int row = index / columns;
      int column = index % columns;
      double x = 12D + column * (76D / Math.max(columns - 1, 1));
      double y = band.start() + row * ((band.end() - band.start()) / Math.max(rows - 1, 1));
      slot.setXCoordinate(percent(x));
      slot.setYCoordinate(percent(y));
      stats.slotCoordinatesSeeded++;
    }
  }

  private Band band(int zoneIndex) {
    return switch (zoneIndex) {
      case 0 -> new Band(18D, 30D);
      case 1 -> new Band(38D, 52D);
      case 2 -> new Band(62D, 78D);
      default -> new Band(82D, 92D);
    };
  }

  private BigDecimal percent(double value) {
    double clamped = Math.max(5D, Math.min(95D, value));
    return BigDecimal.valueOf(clamped).setScale(2, RoundingMode.HALF_UP);
  }

  private UUID floorId(Slot slot) {
    if (slot.getFloor() != null) {
      return slot.getFloor().getId();
    }
    if (slot.getZone().getFloor() != null) {
      return slot.getZone().getFloor().getId();
    }
    return null;
  }

  private boolean hasCompleteCoordinate(Slot slot) {
    return slot.getXCoordinate() != null && slot.getYCoordinate() != null;
  }

  private Comparator<Zone> zoneComparator() {
    return Comparator.comparing(Zone::getName, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(Zone::getCode, String.CASE_INSENSITIVE_ORDER)
        .thenComparing(Zone::getId);
  }

  private Comparator<Slot> slotComparator() {
    return (left, right) -> compareNaturally(left.getCode(), right.getCode());
  }

  private int compareNaturally(String left, String right) {
    List<String> leftTokens = naturalTokens(left);
    List<String> rightTokens = naturalTokens(right);
    int length = Math.min(leftTokens.size(), rightTokens.size());
    for (int i = 0; i < length; i++) {
      String leftToken = leftTokens.get(i);
      String rightToken = rightTokens.get(i);
      int result = compareToken(leftToken, rightToken);
      if (result != 0) {
        return result;
      }
    }
    return Integer.compare(leftTokens.size(), rightTokens.size());
  }

  private int compareToken(String left, String right) {
    boolean leftNumber = Character.isDigit(left.charAt(0));
    boolean rightNumber = Character.isDigit(right.charAt(0));
    if (leftNumber && rightNumber) {
      return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
    }
    return left.compareToIgnoreCase(right);
  }

  private List<String> naturalTokens(String value) {
    String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
    Matcher matcher = NATURAL_TOKEN.matcher(normalized);
    List<String> tokens = new ArrayList<>();
    while (matcher.find()) {
      tokens.add(matcher.group());
    }
    return tokens;
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private boolean isExternalUrl(String value) {
    if (!hasText(value)) {
      return false;
    }
    String normalized = value.trim().toLowerCase(Locale.ROOT);
    return normalized.startsWith("http://") || normalized.startsWith("https://");
  }

  private record DemoMapAsset(String fileName, String contentType) {}

  private record Band(double start, double end) {}

  private static class SeedStats {
    int floorMapsSeeded;
    int floorMapsSkippedConfigured;
    int slotCoordinatesSeeded;
    int slotCoordinatesSkippedConfigured;
    boolean floorMapsSkippedMissingStorage;
  }
}
