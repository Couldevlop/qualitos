package com.openlab.qualitos.industry.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Industry Pack — declarative bundle activated per-tenant.
 *
 * <p>Pure domain record (no Spring / no JPA). Immutable.
 *
 * <p>CLAUDE.md §5 — Domain Adapter Layer. The pack is the ONLY mechanism by which
 * sector-specific logic enters the platform: no hard-coded sector branches in
 * core code (rule 18.2.9).
 *
 * @param id          stable identifier (e.g. {@code manufacturing}, {@code healthcare-hospital})
 * @param version     SemVer (e.g. {@code 1.0.0})
 * @param name        human-readable name
 * @param sectors     ISIC / NACE sector codes covered
 * @param supportedNorms IDs of {@link Norm} the pack pre-configures
 * @param kpis        catalog of KPIs shipped by the pack
 * @param glossary    domain terms by code
 * @param connectors  native connectors declared by the pack
 * @param ishikawaTemplates Ishikawa diagram templates (6M/7M/8M variants)
 * @param pokaYokeLibrary  poka-yoke device patterns
 * @param trainingPaths    onboarding/training programs
 * @param documentsTemplates document templates (procedure, audit, manual…)
 * @param publishedAt publication instant
 * @param sha256      SHA-256 fingerprint of the source YAML (integrity)
 */
public record IndustryPack(
    String id,
    String version,
    String name,
    List<String> sectors,
    List<String> supportedNorms,
    List<KpiDefinition> kpis,
    Map<String, String> glossary,
    List<ConnectorRef> connectors,
    List<IshikawaTemplate> ishikawaTemplates,
    List<PokaYokeDevice> pokaYokeLibrary,
    List<TrainingPath> trainingPaths,
    List<DocumentTemplate> documentsTemplates,
    Instant publishedAt,
    String sha256
) {

  public IndustryPack {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(version, "version");
    Objects.requireNonNull(name, "name");
    sectors = sectors == null ? List.of() : List.copyOf(sectors);
    supportedNorms = supportedNorms == null ? List.of() : List.copyOf(supportedNorms);
    kpis = kpis == null ? List.of() : List.copyOf(kpis);
    glossary = glossary == null ? Map.of() : Map.copyOf(glossary);
    connectors = connectors == null ? List.of() : List.copyOf(connectors);
    ishikawaTemplates = ishikawaTemplates == null ? List.of() : List.copyOf(ishikawaTemplates);
    pokaYokeLibrary = pokaYokeLibrary == null ? List.of() : List.copyOf(pokaYokeLibrary);
    trainingPaths = trainingPaths == null ? List.of() : List.copyOf(trainingPaths);
    documentsTemplates = documentsTemplates == null ? List.of() : List.copyOf(documentsTemplates);
  }

  public String coordinate() {
    return id + "@" + version;
  }
}
