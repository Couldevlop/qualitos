package com.openlab.qualitos.quality.riskregister.application;

import com.openlab.qualitos.quality.riskregister.domain.OpportunityActionStatus;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityDecision;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityLevel;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityStatus;
import com.openlab.qualitos.quality.riskregister.domain.RegisterEventType;
import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
import com.openlab.qualitos.quality.riskregister.domain.RegisterRequirement;
import com.openlab.qualitos.quality.riskregister.domain.RegisterType;
import com.openlab.qualitos.quality.riskregister.domain.RiskCapaKind;
import com.openlab.qualitos.quality.riskregister.domain.RiskDecision;
import com.openlab.qualitos.quality.riskregister.domain.RiskLevel;
import com.openlab.qualitos.quality.riskregister.domain.RiskStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Les vues et commandes du registre.
 *
 * <p>Scores et niveaux sont calculés ici, pas à l'écran : un niveau « Critique »
 * présenté en revue de direction ne doit pas dépendre du navigateur qui l'affiche.
 */
public final class RiskRegisterDto {

    private RiskRegisterDto() {}

    public record RiskView(
            UUID id, String reference, String title, RegisterType type, String process, String site,
            String owner, String cause, String effect, RegisterOrigin origin, String originRef,
            UUID sourceId, int grossSeverity, int grossProbability, int grossScore, RiskLevel grossLevel,
            Integer residualSeverity, Integer residualProbability, Integer residualScore,
            RiskLevel residualLevel, RiskDecision decision, RiskStatus status,
            List<RegisterRequirement> requirements, LocalDate nextReviewOn,
            String effectivenessCriterion, Instant createdAt, Instant updatedAt) {}

    /** La fiche d'un risque : la ligne, ses CAPA liées, son suivi. */
    public record RiskSheet(RiskView risk, List<CapaView> capas, List<EventView> events) {}

    /** Une CAPA liée : sa nature et son responsable d'action, que la fiche montre. */
    public record CapaView(UUID id, String title, LocalDate dueDate, String status,
                           RiskCapaKind kind, String assignee) {}

    public record OpportunityView(
            UUID id, String reference, String title, RegisterType type, String process, String site,
            String owner, LocalDate targetDate, String context, String benefit,
            RegisterOrigin origin, String originRef, int gain, int feasibility, int score,
            OpportunityLevel level, OpportunityDecision decision, OpportunityStatus status,
            List<RegisterRequirement> requirements, String benefitCriterion,
            Instant createdAt, Instant updatedAt) {}

    public record OpportunitySheet(OpportunityView opportunity, List<ActionView> actions,
                                   List<EventView> events) {}

    public record ActionView(UUID id, int number, String title, LocalDate dueDate,
                             OpportunityActionStatus status) {}

    public record EventView(UUID id, RegisterEventType type, String fromValue, String toValue,
                            String detail, Instant at) {}

    /** Les valeurs déjà employées, que l'écran propose en saisie. */
    public record Suggestions(List<String> processes, List<String> sites, List<String> owners) {}

    /**
     * Le brouillon d'un risque issu d'un objet de la plateforme.
     *
     * <p>{@code existing} liste les risques déjà créés depuis cet objet : on les
     * montre plutôt que d'en créer un second sans le savoir.
     */
    public record RiskDraft(
            RegisterOrigin origin, UUID sourceId, String originRef, String title, String cause,
            String effect, Integer grossSeverity, Integer grossProbability, String process,
            boolean eligible, String reason, List<RiskLink> existing) {}

    public record RiskLink(UUID id, String reference) {}

    public record RiskCommand(
            String title, RegisterType type, String process, String site, String owner,
            String cause, String effect, RegisterOrigin origin, String originRef, UUID sourceId,
            Integer grossSeverity, Integer grossProbability,
            Integer residualSeverity, Integer residualProbability,
            RiskDecision decision, RiskStatus status, List<RegisterRequirement> requirements,
            LocalDate nextReviewOn, String effectivenessCriterion) {}

    public record OpportunityCommand(
            String title, RegisterType type, String process, String site, String owner,
            LocalDate targetDate, String context, String benefit, RegisterOrigin origin,
            String originRef, Integer gain, Integer feasibility, OpportunityDecision decision,
            OpportunityStatus status, List<RegisterRequirement> requirements,
            String benefitCriterion) {}

    public record CapaCommand(String title, String description, RiskCapaKind kind, String assignee,
                              LocalDate dueDate) {}

    public record ActionCommand(String title, LocalDate dueDate, OpportunityActionStatus status) {}
}
