package com.openlab.qualitos.quality.capa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class CapaDto {

    private CapaDto() {}

    public record CreateCaseRequest(
            @NotBlank @Size(max = 255) String title,
            String description,
            @NotNull CapaType type,
            @NotNull CapaCriticity criticity,
            @NotNull CapaSourceType sourceType,
            @Size(max = 255) String sourceRef,
            @NotNull UUID ownerId,
            UUID rootCauseId,
            LocalDate dueDate
    ) {}

    public record UpdateCaseRequest(
            @Size(max = 255) String title,
            String description,
            CapaCriticity criticity,
            @Size(max = 255) String sourceRef,
            UUID rootCauseId,
            LocalDate dueDate
    ) {}

    public record EffectivenessRequest(@NotNull Boolean effective) {}

    public record ActionRequest(
            @NotBlank @Size(max = 255) String title,
            String description,
            CapaActionStatus status,
            UUID assigneeId,
            LocalDate dueDate
    ) {}

    public record CaseResponse(
            UUID id,
            UUID tenantId,
            String title,
            String description,
            CapaType type,
            CapaCriticity criticity,
            CapaStatus status,
            CapaSourceType sourceType,
            String sourceRef,
            UUID ownerId,
            UUID rootCauseId,
            LocalDate dueDate,
            Instant resolvedAt,
            Instant closedAt,
            Boolean effectivenessVerified,
            Instant effectivenessVerifiedAt,
            Instant createdAt,
            Instant updatedAt,
            List<ActionResponse> actions
    ) {}

    public record ActionResponse(
            UUID id,
            UUID capaId,
            String title,
            String description,
            CapaActionStatus status,
            UUID assigneeId,
            LocalDate dueDate,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt
    ) {}

    /** Action corrective/préventive suggérée par l'IA (à valider/ajouter). §4.2 */
    public record SuggestedAction(
            String title,
            String description
    ) {}
}
