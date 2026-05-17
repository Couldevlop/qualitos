package com.openlab.qualitos.industry.domain.port;

import com.openlab.qualitos.industry.domain.model.ApplyResult;
import com.openlab.qualitos.industry.domain.model.IndustryPack;
import com.openlab.qualitos.industry.domain.model.ValidationResult;

import java.util.List;
import java.util.UUID;

/**
 * Service Provider Interface (SPI) for Industry Packs.
 *
 * <p>Discovered via {@link java.util.ServiceLoader} so a new sector can be
 * added by dropping a JAR + a {@code META-INF/services} entry — no core code
 * change required (cf. CLAUDE.md §5.1 Domain Adapter Layer).
 *
 * <p>Implementations MUST be pure (no I/O in {@code id/version/sectors}) and
 * thread-safe.
 */
public interface IndustryPackProvider {

  /** Stable pack id, e.g. {@code manufacturing}. */
  String id();

  /** SemVer version of the pack. */
  String version();

  /** ISIC / NACE sector codes supported. */
  List<String> sectors();

  /** Norm identifiers pre-configured by the pack. */
  List<String> supportedNorms();

  /** Returns the immutable {@link IndustryPack} bundle. */
  IndustryPack getPack();

  /** Static structural validation (called at load time). */
  ValidationResult validate();

  /** Apply the pack to a tenant — returns an audit-trail entry. */
  ApplyResult apply(UUID tenantId, String activatedBy);
}
