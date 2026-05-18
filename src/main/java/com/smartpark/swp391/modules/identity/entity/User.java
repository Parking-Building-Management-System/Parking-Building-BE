package com.smartpark.swp391.modules.identity.entity;

import com.smartpark.swp391.infrastructure.persistence.BaseEntity;
import com.smartpark.swp391.modules.identity.enumType.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "users")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("is_deleted = false")
public class User extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Column(nullable = false, unique = true, length = 255)
  private String username;

  @Column(nullable = false, length = 255)
  private String password;

  @Column(name = "full_name", nullable = false, length = 255)
  private String fullName;

  @Column(length = 20)
  private String phone;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  @Builder.Default
  private UserStatus status = UserStatus.ACTIVE;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "is_deleted", nullable = false)
  @Builder.Default
  private boolean isDeleted = false;
}
