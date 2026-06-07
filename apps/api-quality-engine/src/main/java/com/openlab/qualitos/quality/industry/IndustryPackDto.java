package com.openlab.qualitos.quality.industry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class IndustryPackDto {

    private IndustryPackDto() {}

    public record PackResponse(
            UUID id,
            String code,
            String name,
            String description,
            String version,
            String locale,
            List<String> tags,
            // manifest brut renvoyé en JSON pour permettre au front d'explorer
            // les sections (standards, kpis, glossary…) sans appel supplémentaire.
            String manifestJson,
            Instant createdAt,
            Instant updatedAt
    ) {}

    /**
     * H2 (OWASP A01) : {@code activatedBy} N'EST PLUS un champ entrant. L'acteur de
     * l'activation est dérivé côté serveur du {@code sub} du JWT (cf. service), jamais
     * du body falsifiable. Le DTO ne porte plus que la configuration métier.
     */
    public record ActivateRequest(
            /** Overrides JSON optionnels (KPIs custom, glossaire) — passé en string. */
            String configOverridesJson
    ) {}

    public record ActivationResponse(
            UUID id,
            UUID tenantId,
            String packCode,
            ActivationStatus status,
            UUID activatedBy,
            Instant activatedAt,
            Instant deactivatedAt,
            UUID deactivatedBy,
            // Provisionnement Phase 2 (ADR 0019) — champs ADDITIFS, rétro-compatibles.
            // Nuls pour les réponses qui ne provisionnent pas (désactivation, etc.).
            Provisioning provisioning
    ) {}

    /**
     * Bilan du provisionnement de contenu à l'activation (Phase 2, KPIs uniquement).
     * {@code kpisCreated} : KPIs riches matérialisés en KpiDefinition du tenant ;
     * {@code kpisSkipped} : KPIs ignorés (déjà présents / sans kpi_id / échec) ;
     * {@code warnings} : messages non bloquants (collisions, mapping, manifeste illisible).
     */
    public record Provisioning(
            int kpisCreated,
            int kpisSkipped,
            List<String> warnings
    ) {}
}
