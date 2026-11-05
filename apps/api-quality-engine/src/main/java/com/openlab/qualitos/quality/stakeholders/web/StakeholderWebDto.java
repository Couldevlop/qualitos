package com.openlab.qualitos.quality.stakeholders.web;

import com.openlab.qualitos.quality.stakeholders.application.StakeholderPorts;
import com.openlab.qualitos.quality.stakeholders.application.StakeholderService;
import com.openlab.qualitos.quality.stakeholders.domain.ImpactLevel;
import com.openlab.qualitos.quality.stakeholders.domain.MandatoryNature;
import com.openlab.qualitos.quality.stakeholders.domain.Person;
import com.openlab.qualitos.quality.stakeholders.domain.Relevance;
import com.openlab.qualitos.quality.stakeholders.domain.RequirementLink;
import com.openlab.qualitos.quality.stakeholders.domain.RequirementStatus;
import com.openlab.qualitos.quality.stakeholders.domain.Stakeholder;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderCategory;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderRequirement;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderSide;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Les corps de requête du registre des parties intéressées.
 *
 * <p>Aucun ne porte de client ni d'acteur : les deux viennent du jeton. Les
 * bornes ici arrêtent un corps démesuré avant le service ; les règles fines
 * sont au domaine.
 */
public final class StakeholderWebDto {

    private StakeholderWebDto() {}

    public record PersonRequest(@NotNull UUID accountId,
                                @NotBlank @Size(max = Person.NAME_MAX) String name) {
        Person toDomain() {
            return new Person(accountId, name);
        }
    }

    /** L'inscription : trois champs, on analyse ensuite. */
    public record CreateRequest(@NotBlank @Size(max = Stakeholder.NAME_MAX) String name,
                                @NotNull StakeholderCategory category,
                                @NotNull StakeholderSide side) {}

    /** La fiche enregistrée d'un bloc. */
    public record UpdateRequest(@Size(max = Stakeholder.NAME_MAX) String name,
                                StakeholderCategory category,
                                StakeholderSide side,
                                @Size(max = Stakeholder.TEXT_MAX) String description,
                                @Size(max = Stakeholder.LINE_MAX) String contact,
                                @Size(max = Stakeholder.LINE_MAX) String sites,
                                @Size(max = Stakeholder.TEXT_MAX) String needs,
                                @Size(max = Stakeholder.TEXT_MAX) String requirementsSummary,
                                Relevance relevance,
                                @Size(max = Stakeholder.TEXT_MAX) String relevanceJustification,
                                @Size(max = Stakeholder.TEXT_MAX) String risksAndOpportunities,
                                ImpactLevel qualityImpact,
                                ImpactLevel regulatoryImpact,
                                @Valid PersonRequest owner,
                                @Size(max = Stakeholder.TEXT_MAX) String controls,
                                @Size(max = Stakeholder.LINE_MAX) String indicators,
                                @Size(max = Stakeholder.LINE_MAX) String documents,
                                @Size(max = Stakeholder.LINE_MAX) String actions,
                                LocalDate nextReviewOn) {

        StakeholderService.Update toDomain() {
            return new StakeholderService.Update(name, category, side, description, contact, sites,
                    needs, requirementsSummary, relevance, relevanceJustification,
                    risksAndOpportunities, qualityImpact, regulatoryImpact,
                    owner == null ? null : owner.toDomain(),
                    controls, indicators, documents, actions, nextReviewOn);
        }
    }

    /** Une exigence ajoutée à la main, ou validée depuis une proposition de l'IA. */
    public record RequirementRequest(@Size(max = StakeholderRequirement.SHORT_MAX) String need,
                                     @NotBlank @Size(max = StakeholderRequirement.SHORT_MAX) String requirement,
                                     @Size(max = StakeholderRequirement.SHORT_MAX) String origin,
                                     MandatoryNature nature,
                                     @Size(max = StakeholderRequirement.SHORT_MAX) String risk,
                                     ImpactLevel riskLevel,
                                     @Size(max = StakeholderRequirement.SHORT_MAX) String control,
                                     @Size(max = StakeholderRequirement.SHORT_MAX) String indicator,
                                     @Size(max = StakeholderRequirement.SHORT_MAX) String responsible,
                                     @Size(max = StakeholderRequirement.SHORT_MAX) String evidence,
                                     RequirementStatus status) {

        StakeholderRequirement toDomain() {
            return new StakeholderRequirement(null, need, requirement, origin, nature, risk, riskLevel,
                    control, indicator, responsible, evidence, status, null, null, null, null, null);
        }
    }

    public record StatusRequest(@NotNull RequirementStatus status) {}

    public record LinkRequest(@NotNull RequirementLink link, @NotNull UUID target) {}

    /** L'ouverture d'une CAPA pour une exigence qui n'est pas tenue. */
    public record OpenCapaRequest(@NotBlank @Size(max = 255) String title,
                                  @NotNull UUID ownerId,
                                  @Size(max = 255) String ownerName,
                                  @NotNull LocalDate dueDate) {}

    /** Les deux notes d'une ligne de la matrice d'évaluation. */
    public record RatingRequest(@NotNull ImpactLevel qualityImpact,
                                @NotNull ImpactLevel regulatoryImpact) {

        StakeholderService.Rating toDomain() {
            return new StakeholderService.Rating(qualityImpact, regulatoryImpact);
        }
    }

    /** La matrice enregistrée d'un coup : on y compare les lignes entre elles. */
    public record RateRequest(@NotNull @Size(min = 1, max = 500)
                              Map<UUID, @Valid @NotNull RatingRequest> ratings) {

        Map<UUID, StakeholderService.Rating> toDomain() {
            return ratings.entrySet().stream().collect(java.util.stream.Collectors.toMap(
                    Map.Entry::getKey, e -> e.getValue().toDomain()));
        }
    }

    /** Ce que l'assistant lit pour proposer des parties intéressées. */
    public record SuggestRequest(@NotBlank @Size(max = 200) String sector,
                                 @Size(max = 1000) String characteristics) {}

    /** Le texte d'un document à analyser. Il est transmis, jamais stocké. */
    public record ExtractRequest(@Size(max = 255) String documentName,
                                 @NotBlank @Size(max = StakeholderPorts.Assistant.DOCUMENT_MAX) String documentText) {}

    /** Les deux versions d'un même document. */
    public record ChangesRequest(@Size(max = 255) String documentName,
                                 @NotBlank @Size(max = StakeholderPorts.Assistant.COMPARED_MAX) String previousText,
                                 @NotBlank @Size(max = StakeholderPorts.Assistant.COMPARED_MAX) String currentText) {}

    /** Les exigences qu'un changement remet en cause, désignées par un humain. */
    public record ReassessRequest(@NotNull @NotEmpty @Size(max = Stakeholder.REQUIREMENTS_MAX)
                                  List<@NotNull UUID> requirementIds) {}
}
