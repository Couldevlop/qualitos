package com.openlab.qualitos.quality.stakeholders.application;

import com.openlab.qualitos.quality.stakeholders.domain.ImpactLevel;
import com.openlab.qualitos.quality.stakeholders.domain.MandatoryNature;
import com.openlab.qualitos.quality.stakeholders.domain.Person;
import com.openlab.qualitos.quality.stakeholders.domain.Relevance;
import com.openlab.qualitos.quality.stakeholders.domain.RequirementStatus;
import com.openlab.qualitos.quality.stakeholders.domain.Stakeholder;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderCategory;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderRequirement;
import com.openlab.qualitos.quality.stakeholders.domain.StakeholderSide;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Ce que les écrans lisent du registre des parties intéressées. */
public final class StakeholderDto {

    private StakeholderDto() {}

    /**
     * Une ligne du registre : ce que le tableau montre, sans le détail de la
     * fiche.
     *
     * @param criticality calculée, jamais stockée — un impact modifié ne laisse
     *                    pas derrière lui une criticité périmée
     * @param reviewOverdue la date de réévaluation est passée
     */
    public record Row(UUID id, String name, StakeholderCategory category, StakeholderSide side,
                      ImpactLevel qualityImpact, ImpactLevel regulatoryImpact, ImpactLevel criticality,
                      Relevance relevance, LocalDate lastReviewedOn, LocalDate nextReviewOn,
                      boolean reviewOverdue, int requirementCount) {

        public static Row of(Stakeholder s, LocalDate today) {
            return new Row(s.id(), s.name(), s.category(), s.side(), s.qualityImpact(),
                    s.regulatoryImpact(), s.criticality(), s.relevance(), s.lastReviewedOn(),
                    s.nextReviewOn(), s.reviewOverdue(today), s.requirements().size());
        }
    }

    /** La fiche complète, dans l'ordre où elle se remplit. */
    public record Sheet(UUID id, String name, StakeholderCategory category, StakeholderSide side,
                        String description, String contact, String sites,
                        String needs, String requirementsSummary, Relevance relevance,
                        String relevanceJustification, String risksAndOpportunities,
                        ImpactLevel qualityImpact, ImpactLevel regulatoryImpact, ImpactLevel criticality,
                        Person owner, String controls, String indicators, String documents,
                        String actions, LocalDate nextReviewOn, LocalDate lastReviewedOn,
                        boolean reviewOverdue, List<Requirement> requirements) {

        public static Sheet of(Stakeholder s, LocalDate today) {
            return new Sheet(s.id(), s.name(), s.category(), s.side(), s.description(), s.contact(),
                    s.sites(), s.needs(), s.requirementsSummary(), s.relevance(),
                    s.relevanceJustification(), s.risksAndOpportunities(), s.qualityImpact(),
                    s.regulatoryImpact(), s.criticality(), s.owner(), s.controls(), s.indicators(),
                    s.documents(), s.actions(), s.nextReviewOn(), s.lastReviewedOn(),
                    s.reviewOverdue(today),
                    s.requirements().stream().map(Requirement::of).toList());
        }
    }

    /** Une exigence telle que la fiche l'affiche. */
    public record Requirement(UUID id, String need, String requirement, String origin,
                              MandatoryNature nature, String risk, ImpactLevel riskLevel,
                              String control, String indicator, String responsible, String evidence,
                              RequirementStatus status, UUID riskId, UUID ncId, UUID auditId,
                              UUID documentId, UUID capaId) {

        public static Requirement of(StakeholderRequirement r) {
            return new Requirement(r.id(), r.need(), r.requirement(), r.origin(), r.nature(), r.risk(),
                    r.riskLevel(), r.control(), r.indicator(), r.responsible(), r.evidence(),
                    r.status(), r.riskId(), r.ncId(), r.auditId(), r.documentId(), r.capaId());
        }
    }

    /**
     * Les dénombrements du registre, pour les tuiles d'en-tête.
     *
     * @param critical criticité élevée — ce sur quoi la revue de direction s'arrête
     * @param overdue  revues en retard : le registre n'est pas à jour
     * @param toDetermine pertinence encore non tranchée — un périmètre inachevé
     * @param requirementsToReassess exigences à réévaluer, toutes parties confondues
     */
    public record Statistics(long total, long internal, long external, long critical, long overdue,
                             long toDetermine, long requirementsToReassess) {}
}
