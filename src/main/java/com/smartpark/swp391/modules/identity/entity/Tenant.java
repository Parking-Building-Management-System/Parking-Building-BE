package com.smartpark.swp391.modules.identity.entity;

import com.smartpark.swp391.infrastructure.persistence.BaseEntity;
import com.smartpark.swp391.modules.identity.enumType.TenantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "tenants")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
// Soft delete --> vẫn giữ data trong database, chỉ toggle flag này thôi.
// Khi query thì ignore những row có is_deleted = false cải thiện performance (coi như đã xoá)
@SQLRestriction("is_deleted = false")
public class Tenant extends BaseEntity {

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false, length = 100, unique = true)
  private String slug;

  @Column(name = "email_contact", nullable = false, length = 255)
  private String emailContact;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private TenantStatus status = TenantStatus.ACTIVE;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private boolean isDeleted = false;
}
