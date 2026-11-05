package com.openlab.qualitos.quality.stakeholders.infrastructure;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * La fiche d'une partie intéressée, telle qu'elle est stockée.
 *
 * <p>L'identifiant est celui du domaine : la fiche naît avec le sien, et le
 * laisser générer par la base rendrait l'agrégat dépendant d'un aller-retour.
 */
@Entity
@Table(name = "stakeholders")
@Getter
@Setter
@NoArgsConstructor
public class StakeholderJpaEntity {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private com.openlab.qualitos.quality.stakeholders.domain.StakeholderCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private com.openlab.qualitos.quality.stakeholders.domain.StakeholderSide side;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String contact;

    @Column(length = 500)
    private String sites;

    @Column(columnDefinition = "TEXT")
    private String needs;

    @Column(name = "requirements_summary", columnDefinition = "TEXT")
    private String requirementsSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private com.openlab.qualitos.quality.stakeholders.domain.Relevance relevance;

    @Column(name = "relevance_justification", columnDefinition = "TEXT")
    private String relevanceJustification;

    @Column(name = "risks_and_opportunities", columnDefinition = "TEXT")
    private String risksAndOpportunities;

    @Enumerated(EnumType.STRING)
    @Column(name = "quality_impact", nullable = false, length = 10)
    private com.openlab.qualitos.quality.stakeholders.domain.ImpactLevel qualityImpact;

    @Enumerated(EnumType.STRING)
    @Column(name = "regulatory_impact", nullable = false, length = 10)
    private com.openlab.qualitos.quality.stakeholders.domain.ImpactLevel regulatoryImpact;

    /** Le compte du responsable interne de la relation. */
    @Column(name = "owner_account_id")
    private UUID ownerAccountId;

    /** Son nom, recopié : la fiche reste lisible quand le compte disparaît. */
    @Column(name = "owner_name", length = 255)
    private String ownerName;

    @Column(columnDefinition = "TEXT")
    private String controls;

    @Column(length = 500)
    private String indicators;

    @Column(length = 500)
    private String documents;

    @Column(length = 500)
    private String actions;

    @Column(name = "next_review_on")
    private LocalDate nextReviewOn;

    @Column(name = "last_reviewed_on")
    private LocalDate lastReviewedOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    /**
     * Les exigences, en cascade : elles n'existent pas hors de leur fiche, et
     * l'ordre d'affichage est celui de leur inscription.
     */
    @OneToMany(mappedBy = "stakeholder", cascade = CascadeType.ALL, orphanRemoval = true,
               fetch = FetchType.EAGER)
    @OrderBy("position ASC")
    private List<StakeholderRequirementJpaEntity> requirements = new ArrayList<>();
}
