package com.smartpark.swp391.infrastructure.persistence;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

/**
 * Custom Hibernate IdentifierGenerator that generates UUID v7 (time-ordered).
 *
 * <p>UUID v7 provides better database indexing performance compared to UUID v4 because the
 * time-ordered component ensures chronological ordering, which is beneficial for B-tree indexes.
 */
public class UuidV7Generator implements IdentifierGenerator {

  @Override
  public Object generate(SharedSessionContractImplementor session, Object object) {
    return UuidV7.random();
  }
}
