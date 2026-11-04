package com.openlab.qualitos.quality.stakeholders.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Une exigence portée par une partie intéressée.
 *
 * <p>Table fille et non colonne JSON : une exigence se compte, se filtre par
 * statut et se rattache à un risque, une non-conformité, un audit, un document
 * ou une CAPA. Un JSON interdirait de répondre à « quelles exigences sont à
 * réévaluer, toutes parties confondues ? » sans tout relire.
 */
@Entity
@Table(name = "stakeholder_requirements")
@Getter
@Setter
@NoArgsConstructor
public class StakeholderRequirementJpaEntity {

    @Id
    private UUID id;

    /**
     * Recopié de la fiche parente, jamais lu pour filtrer : c'est cette colonne
     * qui place la table sous la politique de cloisonnement PostgreSQL, posée
     * dynamiquement sur toute table qui en porte une (ADR 0086). Le filtre
     * applicatif passe, lui, par l'agrégat parent.
     */
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stakeholder_id", nullable = false)
    private StakeholderJpaEntity stakeholder;

    /** L'ordre d'inscription : celui dans lequel la fiche les affiche. */
    @Column(nullable = false)
    private int position;

    @Column(length = 500)
    private String need;

    @Column(nullable = false, length = 500)
    private String requirement;

    @Column(length = 500)
    private String origin;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private com.openlab.qualitos.quality.stakeholders.domain.MandatoryNature nature;

    @Column(length = 500)
    private String risk;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 10)
    private com.openlab.qualitos.quality.stakeholders.domain.ImpactLevel riskLevel;

    @Column(name = "control_measure", length = 500)
    private String control;

    @Column(length = 500)
    private String indicator;

    @Column(length = 500)
    private String responsible;

    @Column(length = 500)
    private String evidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private com.openlab.qualitos.quality.stakeholders.domain.RequirementStatus status;

    @Column(name = "risk_id")
    private UUID riskId;

    @Column(name = "nc_id")
    private UUID ncId;

    @Column(name = "audit_id")
    private UUID auditId;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "capa_id")
    private UUID capaId;
}
