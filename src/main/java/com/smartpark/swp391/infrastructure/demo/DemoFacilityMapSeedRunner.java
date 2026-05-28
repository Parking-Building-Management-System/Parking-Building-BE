package com.smartpark.swp391.infrastructure.demo;

import com.smartpark.swp391.infrastructure.demo.config.DemoSeedProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DemoFacilityMapSeedRunner {

  DemoSeedProperties properties;
  FacilityMapDemoSeeder seeder;

  @EventListener(ApplicationReadyEvent.class)
  public void seedFacilityMaps() {
    if (!properties.enabled()) {
      return;
    }

    FacilityMapDemoSeedResult result = seeder.seed();
    log.info(
        "Facility map demo seed complete: floorMapsSeeded={}, "
            + "floorMapsSkippedConfigured={}, slotCoordinatesSeeded={}, "
            + "slotCoordinatesSkippedConfigured={}, floorMapsSkippedMissingStorage={}",
        result.floorMapsSeeded(),
        result.floorMapsSkippedConfigured(),
        result.slotCoordinatesSeeded(),
        result.slotCoordinatesSkippedConfigured(),
        result.floorMapsSkippedMissingStorage());
  }
}
