package com.smartpark.swp391.modules.dashboard.entity;

import com.smartpark.swp391.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "api_traffic_logs")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ApiTrafficLog extends BaseEntity {

  @Column(nullable = false, length = 10)
  private String method;

  @Column(nullable = false, length = 500)
  private String path;

  @Column(name = "status_code", nullable = false)
  private Integer statusCode;

  @Column(name = "duration_ms", nullable = false)
  private Long durationMs;

  @Column(name = "occurred_at", nullable = false)
  @Builder.Default
  private LocalDateTime occurredAt = LocalDateTime.now();
}
