package com.smartpark.swp391.modules.firesafety.entity;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import com.smartpark.swp391.modules.firesafety.enumType.FireInspectionResult;
import com.smartpark.swp391.modules.identity.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Filter;

@Entity
@Table(name = "fire_extinguisher_inspections")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Filter(name = TenantScopedEntity.TENANT_FILTER, condition = "tenant_id = cast(:tenantId as uuid)")
public class FireExtinguisherInspection extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "fire_extinguisher_id", nullable = false)
  private FireExtinguisher fireExtinguisher;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "inspected_by")
  private User inspectedBy;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private FireInspectionResult result;

  @Column(name = "pressure_ok")
  private Boolean pressureOk;

  @Column(name = "seal_ok")
  private Boolean sealOk;

  @Column(name = "location_ok")
  private Boolean locationOk;

  @Column(name = "expiry_ok")
  private Boolean expiryOk;

  @Column(name = "photo_url", columnDefinition = "text")
  private String photoUrl;

  @Column(name = "photo_object_key", columnDefinition = "text")
  private String photoObjectKey;

  @Column(columnDefinition = "text")
  private String note;

  @Column(name = "inspected_at", nullable = false)
  private LocalDateTime inspectedAt;

  @Column(name = "next_inspection_at")
  private LocalDateTime nextInspectionAt;
}
