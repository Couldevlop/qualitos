package com.openlab.qualitos.industry.domain.model;

import java.util.List;

/**
 * Sector-specific poka-yoke (mistake-proofing) device pattern.
 * Used by the DMAIC + Poka-Yoke module recommendation engine.
 */
public record PokaYokeDevice(
    String id,
    String name,
    String type,              // contact | constant-number | motion-step
    String category,          // prevention | detection | shut-out | warning
    String description,
    List<String> appliesTo    // process keywords
) {
  public PokaYokeDevice {
    appliesTo = appliesTo == null ? List.of() : List.copyOf(appliesTo);
  }
}
