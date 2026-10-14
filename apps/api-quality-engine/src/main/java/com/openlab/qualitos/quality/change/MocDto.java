package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.change.domain.ImpactDomain;
import com.openlab.qualitos.quality.change.domain.ImpactLevel;
import com.openlab.qualitos.quality.change.domain.MocClassification;
import com.openlab.qualitos.quality.change.domain.MocHistoryEvent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Contrat HTTP de la gestion des changements (ADR 0087). */
public final class MocDto {

    private MocDto() {}

    // ---------- demandes ----------

    /**
     * Ce que le demandeur saisit. Ni code, ni demandeur, ni statut : le code est
     * attribué par le serveur, le demandeur est l'utilisateur du jeton.
     */
    public record RequestForm(
            @NotBlank @Size(max = 250) String title,
            @NotNull ChangeRequestType type,
            @Size(max = 200) String site,
            @Size(max = 4000) String description,
            @Size(max = 4000) String justification,
            LocalDate plannedFor
    ) {}

    public record ImpactForm(
            @NotNull ImpactLevel level,
            @Size(max = 2000) String notes
    ) {}

    public record DecisionForm(
            @NotNull ApprovalDecision decision,
            @Size(max = 1000) String comment
    ) {}

    /** Une ligne de la liste de gauche. */
    public record Summary(
            UUID id, String code, String title, ChangeRequestType type, ChangeRequestStatus status,
            MocClassification classification, Integer score, String requesterName, Instant createdAt,
            boolean awaitingMe
    ) {}

    /** La demande sélectionnée, avec tout ce que ses quatre onglets montrent. */
    public record Detail(
            UUID id, String code, String title, ChangeRequestType type, String site,
            String description, String justification, LocalDate plannedFor,
            ChangeRequestStatus status, String requesterName, boolean requestedByMe,
            Instant createdAt, Instant submittedAt, Instant closedAt,
            Integer score, MocClassification classification,
            List<ImpactView> impacts, List<ValidationView> validations, List<HistoryView> history
    ) {}

    /** Un domaine : son poids (pour l'affichage), et son évaluation s'il en a une. */
    public record ImpactView(ImpactDomain domain, int weight, ImpactLevel level, String notes) {}

    /**
     * Une carte de validation. {@code preview} : la demande n'est pas encore soumise,
     * la carte montre ce que la classification actuelle exigera (rien n'est figé).
     * {@code holders} : les titulaires COURANTS du rôle, ceux qui peuvent statuer.
     */
    public record ValidationView(
            UUID id, String roleLabel, ApprovalDecision decision, String decidedByName,
            Instant decidedAt, String comment, List<String> holders, boolean heldByMe,
            boolean canDecide, boolean preview
    ) {}

    public record HistoryView(Instant occurredAt, MocHistoryEvent event, String actorName, String detail) {}

    // ---------- paramètres ----------

    public record SettingsView(
            int majorThreshold, int criticalThreshold, int reminderDays,
            Map<ImpactDomain, Integer> weights, List<RoleView> roles
    ) {}

    public record RoleView(UUID id, String label, Set<MocClassification> requiredFor, List<HolderView> holders) {}

    public record HolderView(UUID userId, String label) {}

    public record SettingsForm(
            @NotNull @Min(1) @Max(99) Integer majorThreshold,
            @NotNull @Min(2) @Max(100) Integer criticalThreshold,
            @NotNull @Min(1) @Max(30) Integer reminderDays,
            @NotNull Map<ImpactDomain, @NotNull @Min(0) @Max(10) Integer> weights,
            @NotNull @Size(min = 1, max = 20) List<@NotNull @Valid RoleForm> roles
    ) {}

    public record RoleForm(
            UUID id,
            @NotBlank @Size(max = 100) String label,
            @NotNull Set<@NotNull MocClassification> requiredFor,
            @NotNull @Size(max = 20) List<@NotNull @Valid HolderForm> holders
    ) {}

    public record HolderForm(@NotNull UUID userId, @NotBlank @Size(max = 200) String label) {}
}
