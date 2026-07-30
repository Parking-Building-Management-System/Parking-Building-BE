package com.smartpark.swp391.modules.manager.support;

public enum AnalyticsTrendGranularity {
  HOUR("hour"),
  DAY("day"),
  WEEK("week"),
  MONTH("month");

  private final String sqlValue;

  AnalyticsTrendGranularity(String sqlValue) {
    this.sqlValue = sqlValue;
  }

  public String sqlValue() {
    return sqlValue;
  }
}
