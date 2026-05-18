package com.openlab.qualitos.industry.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Pre-built Ishikawa diagram template (6M / 7M / 8M). The template seeds
 * branches with sector-specific cause hints. The cause-tree is then enriched
 * by the IA service (cf. CLAUDE.md §3.5).
 */
public record IshikawaTemplate(
    String id,
    String name,
    String problemArchetype,
    List<String> branches,                  // e.g. ["Method","Machine","Manpower",...]
    Map<String, List<String>> seedCauses    // branch -> list of cause hints
) {
  public IshikawaTemplate {
    branches = branches == null ? List.of() : List.copyOf(branches);
    seedCauses = seedCauses == null ? Map.of() : Map.copyOf(seedCauses);
  }
}
