package com.openlab.qualitos.quality.airouting.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Le fournisseur d'IA auquel un tenant a épinglé ses appels LLM (ADR 0102, §12.2).
 *
 * <p>Une politique par tenant. Son absence n'est pas un « vide » : le tenant suit
 * alors le défaut de la plateforme ({@code AI_DEFAULT_PROVIDER} de l'ai-service).
 * Le tenant et l'auteur viennent du jeton, jamais du corps (§18.2).
 *
 * <p>Domaine pur : aucune dépendance Spring ni JPA.
 */
public final class TenantAiPolicy {

    private final UUID tenantId;
    private final AiProvider provider;
    private final Instant updatedAt;
    private final UUID updatedBy;

    public TenantAiPolicy(UUID tenantId, AiProvider provider, Instant updatedAt, UUID updatedBy) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
        this.updatedBy = Objects.requireNonNull(updatedBy, "updatedBy");
    }

    public UUID getTenantId() { return tenantId; }
    public AiProvider getProvider() { return provider; }
    public Instant getUpdatedAt() { return updatedAt; }
    public UUID getUpdatedBy() { return updatedBy; }
}
