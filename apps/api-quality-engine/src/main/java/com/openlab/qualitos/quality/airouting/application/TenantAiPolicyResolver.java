package com.openlab.qualitos.quality.airouting.application;

import com.openlab.qualitos.quality.airouting.domain.AiProvider;
import com.openlab.qualitos.quality.airouting.domain.TenantAiPolicy;
import com.openlab.qualitos.quality.airouting.domain.TenantAiPolicyRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Le fournisseur à imposer aux appels LLM d'un tenant (ADR 0102, §12.2).
 *
 * <p>Politique du tenant si elle existe ; sinon rien, et l'ai-service applique le
 * défaut de la plateforme. Un échec de lecture remonte tel quel : un appel dont on
 * ne sait pas s'il a le droit de sortir ne part pas (fail-closed).
 */
public class TenantAiPolicyResolver {

    private final TenantAiPolicyRepository repository;

    public TenantAiPolicyResolver(TenantAiPolicyRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Optional<AiProvider> providerFor(UUID tenantId) {
        Objects.requireNonNull(tenantId, "tenantId");
        return repository.findByTenantId(tenantId).map(TenantAiPolicy::getProvider);
    }
}
