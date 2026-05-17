package com.openlab.qualitos.industry.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * Sectoral KPI definition. CLAUDE.md §6.6 — every KPI must declare its formula,
 * thresholds, source, owner. No KPI displayed without a definition (rule 18.2.8).
 */
public record KpiDefinition(
    String id,
    String name,
    String category,
    String formula,
    String unit,
    String target,
    String thresholdWarning,
    String thresholdCritical,
    String dataSource,
    String refreshFrequency,
    String owner,
    List<String> applicableIndustries,
    List<String> relatedKpis,
    String explainability
) {
  public KpiDefinition {
    Objects.requireNonNull(id, "kpi id");
    Objects.requireNonNull(name, "kpi name");
    Objects.requireNonNull(formula, "kpi formula");
    applicableIndustries = applicableIndustries == null ? List.of() : List.copyOf(applicableIndustries);
    relatedKpis = relatedKpis == null ? List.of() : List.copyOf(relatedKpis);
  }
}
