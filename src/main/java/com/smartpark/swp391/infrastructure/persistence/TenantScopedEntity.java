package com.smartpark.swp391.infrastructure.persistence;

import com.smartpark.swp391.modules.identity.entity.Tenant;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@MappedSuperclass
@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@FilterDef(
    name = TenantScopedEntity.TENANT_FILTER,
    parameters = {@ParamDef(name = TenantScopedEntity.TENANT_ID_PARAM, type = String.class)})
public abstract class TenantScopedEntity extends DomainBaseEntity {
  public static final String TENANT_FILTER = "tenantFilter";
  public static final String TENANT_ID_PARAM = "tenantId";

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;
}
