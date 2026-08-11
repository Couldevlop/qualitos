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
            // Nature de l'action. Absente à la création = CORRECTIVE, le cas de
            // loin le plus fréquent ; absente au PATCH = « ne touche pas ».
            CapaActionType actionType,
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
            LinkedNonConformity sourceNonConformity,
            // Ce qui s'oppose encore à la clôture, énuméré à l'avance. Renseigné
            // sur la FICHE seulement — comme sourceNonConformity, et pour la même
            // raison : la liste paginée n'en montre rien et le calcul coûterait
            // une requête par ligne. Jamais nul sur la fiche : une liste vide dit
            // « rien ne s'y oppose », ce qui est une information.
            List<ClosureBlocker> closureBlockers
    ) {}

    /**
     * Un motif de refus de clôture, énoncé AVANT le clic.
     *
     * <p>Un code et un décompte, pas une phrase : la phrase se construit côté
     * écran, dans la langue de l'utilisateur. Renvoyer un texte tout fait
     * l'aurait figé en français dans une interface qui parle six langues, et
     * aurait obligé le navigateur à afficher un message serveur qu'il ne peut
     * ni traduire ni mettre en forme.
     */
    public record ClosureBlocker(
            ClosureBlockerCode code,
            /** Nombre d'éléments concernés — actions restantes, écarts ouverts… */
            long count
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
            CapaActionType actionType,
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
