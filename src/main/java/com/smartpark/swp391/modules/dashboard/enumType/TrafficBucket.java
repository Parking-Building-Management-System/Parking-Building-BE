package com.smartpark.swp391.modules.dashboard.enumType;

public enum TrafficBucket {
  MINUTE("minute"),
  HOUR("hour"),
  DAY("day");

  private final String postgresUnit;

  TrafficBucket(String postgresUnit) {
    this.postgresUnit = postgresUnit;
  }

  public String postgresUnit() {
    return postgresUnit;
  }
}
