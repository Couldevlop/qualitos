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

    /**
     * Création ET mise à jour partielle d'une action.
     *
     * <p>{@code @NotBlank} ne vaut qu'à la création : la mise à jour est un
     * PATCH, où un champ absent signifie « ne touche pas ». Le contrôleur ne
     * valide donc pas ce record sur le PATCH, et c'est le service qui refuse
     * un libellé vide ou trop long — sans quoi l'édition en ligne du tableau
     * pourrait effacer le libellé d'une action.
     */
    public record ActionRequest(
            @NotBlank @Size(max = 255) String title,
            String description,
            CapaActionStatus status,
            UUID assigneeId,
            @Size(max = 255) String assigneeName,
            LocalDate decidedOn,
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
            List<ActionResponse> actions,
            // Non-conformité dont procède le dossier, donc dont procèdent ses
            // actions. Portée par le dossier et non répétée sur chaque action :
            // c'est une propriété du dossier, et la répéter N fois laisserait
            // croire qu'elle peut différer d'une ligne à l'autre. Nulle quand le
            // dossier ne vient pas d'un écart.
            LinkedNonConformity sourceNonConformity
    ) {}

    /** Écart d'origine, réduit à ce qu'un tableau doit en montrer. */
    public record LinkedNonConformity(
            UUID id,
            String reference,
            String title
    ) {}

    public record ActionResponse(
            UUID id,
            UUID capaId,
            String title,
            String description,
            CapaActionStatus status,
            UUID assigneeId,
            String assigneeName,
            LocalDate decidedOn,
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
