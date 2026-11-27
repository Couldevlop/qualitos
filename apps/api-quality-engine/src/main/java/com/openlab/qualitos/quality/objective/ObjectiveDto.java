package com.openlab.qualitos.quality.objective;

import com.openlab.qualitos.quality.kpi.KpiDirection;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Ce qui circule entre l'écran des objectifs qualité et le serveur.
 *
 * <p>Ni le client ni l'auteur n'y figurent : ils viennent du jeton (règle
 * 18.2 #2). L'unité et le sens d'amélioration ne sont jamais ENVOYÉS — ils
 * appartiennent au KPI — mais ils sont RENDUS, parce que l'écran doit afficher
 * « ≤ 500 ppm » sans aller rechercher la définition lui-même.
 */
public final class ObjectiveDto {

    private ObjectiveDto() {}

    // ---------- écriture ----------

    /**
     * La création d'un objectif, telle que la remplit l'écran « Nouvel objectif
     * qualité ».
     *
     * <p>{@code periodMonths} vaut 1, 3, 6 ou 12 ; le serveur refuse le reste.
     * Les seuils sont dans l'unité du KPI, qui n'est pas répétée ici.
     */
    public record CreateRequest(
            @NotBlank @Size(max = 250) String title,
            @NotNull UUID kpiId,
            UUID processId,
            @Size(max = 120) String processName,
            @Size(max = 40) String isoClause,
            @NotNull @Min(1) Integer periodMonths,
            @NotNull BigDecimal targetValue,
            @NotNull BigDecimal thresholdWarning,
            @NotNull BigDecimal thresholdCritical,
            @NotNull @Min(1) Integer warningPeriods,
            @NotNull @Min(1) Integer criticalPeriods,
            @NotNull @Min(1) Integer recoveryPeriods
    ) {}

    /** La modification d'un objectif : seuls les champs renseignés changent. */
    public record UpdateRequest(
            @Size(max = 250) String title,
            UUID processId,
            @Size(max = 120) String processName,
            @Size(max = 40) String isoClause,
            @Min(1) Integer periodMonths,
            BigDecimal targetValue,
            BigDecimal thresholdWarning,
            BigDecimal thresholdCritical,
            @Min(1) Integer warningPeriods,
            @Min(1) Integer criticalPeriods,
            @Min(1) Integer recoveryPeriods,
            /** Même nom que dans la réponse : « status » y désigne déjà autre chose. */
            ObjectiveLifecycle lifecycle
    ) {}

    /**
     * « Ajouter une action » : ce que le pilote écrit dans la fenêtre CAPA pour
     * traiter l'écart d'un objectif.
     *
     * <p>Rien n'est prérédigé. Le dossier naît de CES mots-là, et le pilote et
     * l'échéance sont ceux qu'on désigne ici — l'objectif n'en porte plus.
     *
     * <p>Ce que la fenêtre n'envoie PAS, et que le serveur impose : la source
     * ({@code QUALITY_OBJECTIVE}), la criticité (celle du niveau atteint), la
     * vérification d'efficacité (exigée, sans question posée) et son critère
     * (verrouillé). Voir l'ADR 0143.
     */
    public record OpenCapaRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 4000) String description,
            @NotNull UUID ownerId,
            @Size(max = 255) String ownerName,
            @NotNull LocalDate dueDate
    ) {}

    /** Le dossier qui vient de naître, et la proposition qu'il solde s'il y en avait une. */
    public record CapaOpenedResponse(UUID capaId, UUID proposalId) {}

    /**
     * Le rejet d'une CAPA proposée. Le motif est OBLIGATOIRE : un rejet sans
     * justification est exactement ce qu'un auditeur ne peut pas instruire.
     */
    public record RejectRequest(@NotBlank @Size(max = 4000) String reason) {}

    // ---------- lecture ----------

    /**
     * Un objectif, ses réglages, et ce que valent ses mesures aujourd'hui.
     *
     * @param kpiUnit      l'unité, reprise du KPI : l'écran l'affiche, l'objectif ne la porte pas
     * @param kpiDirection le sens d'amélioration, repris du KPI
     * @param effectivenessCriterion le critère d'efficacité que portera la CAPA née de cet
     *                     objectif, rédigé par la règle. RENDU et jamais envoyé : la fenêtre
     *                     d'ouverture le montre verrouillé, et le serveur l'écrit lui-même
     *                     (ADR 0133 §4). Le lire sur la ligne évite de le redemander.
     */
    public record ObjectiveResponse(
            UUID id,
            String title,
            UUID kpiId,
            String kpiCode,
            String kpiName,
            String kpiUnit,
            KpiDirection kpiDirection,
            UUID processId,
            String processName,
            String isoClause,
            int periodMonths,
            BigDecimal targetValue,
            BigDecimal thresholdWarning,
            BigDecimal thresholdCritical,
            int warningPeriods,
            int criticalPeriods,
            int recoveryPeriods,
            String effectivenessCriterion,
            ObjectiveLifecycle lifecycle,
            AssessmentResponse assessment,
            Instant createdAt,
            Instant updatedAt
    ) {}

    /** Ce que dit la confrontation de l'objectif à ses mesures. */
    public record AssessmentResponse(
            ObjectiveStatus status,
            BigDecimal observedValue,
            BigDecimal deviationAbsolute,
            BigDecimal deviationPercent,
            int warningBreaches,
            int criticalBreaches,
            int onTargetPeriods,
            ObjectiveLevel triggeredLevel,
            boolean recoveryAchieved,
            boolean heldByOpenCapa,
            int periodsConsidered,
            Instant lastPeriodStart,
            Instant lastPeriodEnd
    ) {}

    /**
     * Une CAPA proposée, prête à être acceptée telle quelle.
     *
     * <p>{@code dueDate} est calculée à l'acceptation, pas ici : une proposition
     * laissée trois semaines en attente donnerait sinon un dossier né en retard.
     */
    public record ProposalResponse(
            UUID id,
            UUID objectiveId,
            String objectiveTitle,
            ObjectiveLevel level,
            Instant periodStart,
            Instant periodEnd,
            BigDecimal observedValue,
            int breachPeriods,
            String title,
            String description,
            String effectivenessCriterion,
            ProposalStatus status,
            UUID capaCaseId,
            String rejectionReason,
            UUID decidedBy,
            Instant decidedAt,
            Instant createdAt
    ) {}

    /**
     * L'écran « Objectifs vs KPI réels » : les compteurs, les lignes, et ce qui
     * attend une décision.
     *
     * @param asOf la fin de la période observée ; {@code null} = l'état courant
     */
    public record ReviewResponse(
            LocalDate asOf,
            Counters counters,
            List<ObjectiveResponse> objectives,
            List<ProposalResponse> pendingProposals
    ) {}

    /** Les quatre tuiles de la revue. */
    public record Counters(int tracked, int onTarget, int warning, int critical) {}
}
