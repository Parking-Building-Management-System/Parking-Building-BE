package com.smartpark.swp391.infrastructure.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.demo-seed")
public record DemoSeedProperties(
    boolean enabled, boolean floorMapsEnabled, boolean slotCoordinatesEnabled) {}
