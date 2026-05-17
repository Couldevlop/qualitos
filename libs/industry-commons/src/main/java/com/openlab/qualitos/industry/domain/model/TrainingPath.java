package com.openlab.qualitos.industry.domain.model;

import java.util.List;

/**
 * Onboarding / certification training path scoped to a role within the
 * industry pack (cf. CLAUDE.md §19.3 gamification: Yellow→Black Belt).
 */
public record TrainingPath(
    String id,
    String name,
    String targetRole,
    String level,                  // yellow | green | black
    Integer durationHours,
    List<String> modules
) {
  public TrainingPath {
    modules = modules == null ? List.of() : List.copyOf(modules);
  }
}
