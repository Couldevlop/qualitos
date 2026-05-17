package com.openlab.qualitos.industry.application.usecase;

import com.openlab.qualitos.industry.domain.model.ApplyResult;
import com.openlab.qualitos.industry.domain.port.IndustryPackProvider;
import com.openlab.qualitos.industry.domain.port.IndustryPackRegistry;

import java.util.Objects;
import java.util.UUID;

/**
 * Use case: activate an Industry Pack for a tenant.
 *
 * <p>OWASP A01: tenantId MUST come from the JWT (caller's responsibility — checked
 * at the presentation layer). This use case only orchestrates registry lookup +
 * provider.apply(); it does not trust the tenantId source itself.
 */
public final class ActivateIndustryPackUseCase {

  private final IndustryPackRegistry registry;

  public ActivateIndustryPackUseCase(IndustryPackRegistry registry) {
    this.registry = Objects.requireNonNull(registry, "registry");
  }

  public ApplyResult activate(UUID tenantId, String packId, String activatedBy) {
    Objects.requireNonNull(tenantId, "tenantId");
    Objects.requireNonNull(packId, "packId");
    Objects.requireNonNull(activatedBy, "activatedBy");

    IndustryPackProvider provider = registry.findProvider(packId)
        .orElseThrow(() -> new IndustryPackNotFoundException(packId));
    return provider.apply(tenantId, activatedBy);
  }

  public static class IndustryPackNotFoundException extends RuntimeException {
    public IndustryPackNotFoundException(String id) {
      super("Industry pack not found: " + id);
    }
  }
}
