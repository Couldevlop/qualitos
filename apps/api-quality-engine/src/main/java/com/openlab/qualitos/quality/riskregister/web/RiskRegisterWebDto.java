package com.openlab.qualitos.quality.riskregister.web;

import com.openlab.qualitos.quality.riskregister.domain.OpportunityActionStatus;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityDecision;
import com.openlab.qualitos.quality.riskregister.domain.OpportunityStatus;
import com.openlab.qualitos.quality.riskregister.domain.RegisterOrigin;
import com.openlab.qualitos.quality.riskregister.domain.RegisterRequirement;
import com.openlab.qualitos.quality.riskregister.domain.RegisterType;
import com.openlab.qualitos.quality.riskregister.domain.RiskCapaKind;
import com.openlab.qualitos.quality.riskregister.domain.RiskDecision;
import com.openlab.qualitos.quality.riskregister.domain.RiskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Les corps de requête du registre.
 *
 * <p>Ces contraintes sont une première barrière, lisible dans l'OpenAPI. Les
 * règles qui croisent deux champs — résiduelle sous la brute, origine ou
 * exigence propre à un registre — sont tenues par le domaine (422 avec le champ).
 *
 * <p>Aucun champ tenant ni auteur : ils viennent du jeton (§18.2). Aucune
 * référence non plus : c'est le serveur qui l'attribue.
 */
public final class RiskRegisterWebDto {

    private RiskRegisterWebDto() {}

    public record RiskRequest(
            @NotBlank @Size(max = 255) String title,
            @NotNull RegisterType type,
            @NotBlank @Size(max = 120) String process,
            @Size(max = 120) String site,
            @NotBlank @Size(max = 150) String owner,
            @Size(max = 4000) String cause,
            @Size(max = 4000) String effect,
            RegisterOrigin origin,
            @Size(max = 120) String originRef,
            UUID sourceId,
            @NotNull @Min(1) @Max(5) Integer grossSeverity,
            @NotNull @Min(1) @Max(5) Integer grossProbability,
            @Min(1) @Max(5) Integer residualSeverity,
            @Min(1) @Max(5) Integer residualProbability,
            RiskDecision decision,
            RiskStatus status,
            @Size(max = 10) List<RegisterRequirement> requirements,
            LocalDate nextReviewOn,
            @Size(max = 1000) String effectivenessCriterion) {}

    public record OpportunityRequest(
            @NotBlank @Size(max = 255) String title,
            @NotNull RegisterType type,
            @NotBlank @Size(max = 120) String process,
            @Size(max = 120) String site,
            @NotBlank @Size(max = 150) String owner,
            LocalDate targetDate,
            @Size(max = 4000) String context,
            @Size(max = 4000) String benefit,
            RegisterOrigin origin,
            @Size(max = 120) String originRef,
            @NotNull @Min(1) @Max(5) Integer gain,
            @NotNull @Min(1) @Max(5) Integer feasibility,
            OpportunityDecision decision,
            OpportunityStatus status,
            @Size(max = 10) List<RegisterRequirement> requirements,
            @Size(max = 1000) String benefitCriterion) {}

    public record CapaRequest(
            @NotBlank @Size(max = 255) String title,
            @Size(max = 4000) String description,
            @NotNull RiskCapaKind kind,
            @NotBlank @Size(max = 255) String assignee,
            @NotNull LocalDate dueDate) {}

    public record ActionRequest(
            @NotBlank @Size(max = 255) String title,
            LocalDate dueDate,
            OpportunityActionStatus status) {}
}
