package com.openlab.qualitos.quality.industry;

import jakarta.validation.constraints.NotNull;

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

    public record ActivateRequest(
            @NotNull UUID activatedBy,
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
            UUID deactivatedBy
    ) {}
}
