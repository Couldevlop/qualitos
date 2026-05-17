package com.openlab.qualitos.industry.infrastructure.external;

import com.openlab.qualitos.industry.domain.model.ApplyResult;
import com.openlab.qualitos.industry.domain.model.IndustryPack;
import com.openlab.qualitos.industry.domain.model.ValidationResult;
import com.openlab.qualitos.industry.domain.port.IndustryPackProvider;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * In-memory {@link IndustryPackProvider} backed by a fully-loaded
 * {@link IndustryPack}. Used by both the SPI ServiceLoader path
 * and the YAML-classpath loader path.
 */
public final class YamlIndustryPackProvider implements IndustryPackProvider {

  private final IndustryPack pack;

  public YamlIndustryPackProvider(IndustryPack pack) {
    if (pack == null) throw new IllegalArgumentException("pack required");
    this.pack = pack;
  }

  @Override public String id() { return pack.id(); }
  @Override public String version() { return pack.version(); }
  @Override public List<String> sectors() { return pack.sectors(); }
  @Override public List<String> supportedNorms() { return pack.supportedNorms(); }
  @Override public IndustryPack getPack() { return pack; }

  @Override
  public ValidationResult validate() {
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();
    if (isBlank(pack.id())) errors.add("pack.id is blank");
    if (isBlank(pack.version())) errors.add("pack.version is blank");
    if (isBlank(pack.name())) errors.add("pack.name is blank");
    if (pack.sectors().isEmpty()) warnings.add("pack.sectors is empty");
    if (pack.kpis().isEmpty()) warnings.add("pack.kpis is empty");
    if (pack.supportedNorms().isEmpty()) warnings.add("pack.supportedNorms is empty");
    return errors.isEmpty()
        ? ValidationResult.ok(warnings)
        : ValidationResult.invalid(errors);
  }

  @Override
  public ApplyResult apply(UUID tenantId, String activatedBy) {
    if (tenantId == null) throw new IllegalArgumentException("tenantId required");
    if (isBlank(activatedBy)) throw new IllegalArgumentException("activatedBy required");
    ValidationResult vr = validate();
    if (!vr.valid()) {
      throw new IllegalStateException("Pack invalid: " + vr.errors());
    }
    return new ApplyResult(
        tenantId,
        pack.id(),
        pack.version(),
        pack.sha256(),
        Instant.now(),
        pack.connectors().stream().map(c -> c.type() + ":" + c.name()).toList(),
        pack.kpis().stream().map(k -> k.id()).toList(),
        pack.supportedNorms());
  }

  private static boolean isBlank(String s) {
    return s == null || s.isBlank();
  }
}
