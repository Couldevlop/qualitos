package com.openlab.qualitos.quality.ehs.web;

import com.openlab.qualitos.quality.ehs.domain.IncidentSeverity;
import com.openlab.qualitos.quality.ehs.domain.IncidentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * DTOs HTTP — annotations Jakarta Validation pour produire 400 propres au
 * niveau du contrôleur. Reste à la frontière web ; ne remontent pas dans
 * l'application/domaine.
 */
public final class IncidentWebDto {

    private IncidentWebDto() {}

    public record ReportRequest(
            @NotBlank @Size(max = 100)
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._\\-]{1,99}$",
                    message = "code must be alphanumeric kebab/snake (2..100 chars)")
            String code,
            @NotBlank @Size(max = 250) String title,
            @Size(max = 4000) String description,
            @NotNull IncidentType type,
            IncidentSeverity severity,
            Instant occurredAt,
            @Size(max = 500) String location,
            @NotNull UUID reportedBy
    ) {}

    public record EditRequest(
            @Size(max = 250) String title,
            @Size(max = 4000) String description,
            @Size(max = 500) String location,
            @Size(max = 1000) String personsInvolved,
            IncidentSeverity severity,
            @Size(max = 500) String standardsCsv
    ) {}

    public record InvestigateRequest(UUID ownerUserId) {}

    public record MitigateRequest(
            @NotBlank @Size(max = 2000) String rootCause,
            @NotBlank @Size(max = 2000) String correctiveActions
    ) {}

    public record LinkCapaRequest(@NotNull UUID capaCaseId) {}
    public record LinkNcRequest(@NotNull UUID ncId) {}
}
