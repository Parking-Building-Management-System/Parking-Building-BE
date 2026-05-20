package com.smartpark.swp391.infrastructure.tenant;

import com.smartpark.swp391.infrastructure.persistence.TenantScopedEntity;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class TenantFilterAspect {

  EntityManager entityManager;

  @Around("execution(* com.smartpark.swp391.modules..repository..*.*(..))")
  public Object enableTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
    Session session = entityManager.unwrap(Session.class);
    UUID tenantId = TenantContext.getTenantId().orElse(null);

    if (tenantId == null) {
      session.disableFilter(TenantScopedEntity.TENANT_FILTER);
    } else {
      session
          .enableFilter(TenantScopedEntity.TENANT_FILTER)
          .setParameter(TenantScopedEntity.TENANT_ID_PARAM, tenantId.toString());
    }

    return joinPoint.proceed();
  }
}
