package com.smartpark.swp391.modules.identity.entity;

import com.smartpark.swp391.infrastructure.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Permission extends BaseEntity {

  @Column(nullable = false, unique = true, length = 255)
  private String name;

  @Column(length = 100)
  private String scope;

  @Column(length = 100)
  private String module;

  @Column(length = 100)
  private String resource;

  @Column(length = 255)
  private String label;

  @Column(length = 50)
  private String action;
}
