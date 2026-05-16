package com.smartpark.swp391.infrastructure.persistence;

import com.github.f4b6a3.uuid.alt.GUID;
import java.util.UUID;

/**
 * Utility class for generating UUID v7 (time-ordered epoch).
 *
 * <p>UUID v7 provides better database indexing performance compared to UUID v4 because the
 * time-ordered component ensures chronological ordering, which is beneficial for B-tree indexes.
 */
public final class UuidV7 {

  private UuidV7() {
    // Prevent instantiation
  }

  /**
   * Generate a new UUID v7 (time-ordered epoch).
   *
   * @return a new UUID v7
   */
  public static UUID random() {
    return GUID.v7().toUUID();
  }
}
