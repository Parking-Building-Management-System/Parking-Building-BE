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
@Table(name = "roles")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {

  @Column(nullable = false, unique = true, length = 255)
  private String name;

  @Column(length = 1000)
  private String desc;
}
