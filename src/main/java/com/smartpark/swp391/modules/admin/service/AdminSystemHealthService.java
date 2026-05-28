package com.smartpark.swp391.modules.admin.service;

import com.smartpark.swp391.modules.admin.dto.health.ServiceHealthResponse;
import com.smartpark.swp391.modules.admin.dto.health.SystemErrorResponse;
import com.smartpark.swp391.modules.admin.dto.health.SystemHealthSummaryResponse;
import com.smartpark.swp391.modules.admin.dto.health.TopEndpointResponse;
import com.smartpark.swp391.modules.admin.dto.health.TrafficPointResponse;
import java.time.Instant;
import java.util.List;

public interface AdminSystemHealthService {
  SystemHealthSummaryResponse getSummary();

  List<ServiceHealthResponse> getServices();

  List<TrafficPointResponse> getTraffic(Instant from, Instant to, String granularity);

  List<TopEndpointResponse> getTopEndpoints(Instant from, Instant to, int limit);

  List<SystemErrorResponse> getErrors(Instant from, Instant to);
}
