package com.openlab.qualitos.quality.objective;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Une CAPA PROPOSÉE sur écart d'objectif qualité — et pas encore une CAPA.
 *
 * <p>Le logiciel constate l'écart et rédige le dossier ; c'est le pilote qui
 * l'accepte, le rejette avec justification, ou lui ajoute une action. Créer le
 * dossier d'autorité remplirait le registre de CAPA que personne n'a décidées,
 * et l'ISO demande l'inverse : une action décidée par qui en répond (§10.2).
 * C'est aussi l'invariant de la plateforme — l'outil propose, l'humain décide
 * (CLAUDE.md §12).
 *
 * <p>Tant qu'elle est {@code PENDING}, aucun dossier n'existe. Acceptée, elle
 * porte l'identifiant du dossier créé ; rejetée, elle garde la justification —
 * et c'est cette trace que l'auditeur vient lire quand un objectif critique n'a
 * produit aucune CAPA.
 *
 * <p>La période déclenchante et la valeur relevée sont RECOPIÉES : une
 * proposition doit se relire sans recalcul, même des années après, même si les
 * mesures ont été corrigées depuis.
 */
@Entity
@Table(name = "quality_objective_proposals",
        uniqueConstraints = @UniqueConstraint(name = "uk_qo_proposals_periode",
                columnNames = {"tenant_id", "objective_id", "level", "period_start"}),
        indexes = {
                @Index(name = "ix_qo_proposals_attente",
                        columnList = "tenant_id, status, created_at"),
                @Index(name = "ix_qo_proposals_objectif",
                        columnList = "tenant_id, objective_id, created_at")
        })
public class QualityObjectiveProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "objective_id", nullable = false, updatable = false)
    private UUID objectiveId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, updatable = false)
    private ObjectiveLevel level;

    @Column(name = "period_start", nullable = false, updatable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false, updatable = false)
    private Instant periodEnd;

    @Column(name = "observed_value", nullable = false, precision = 24, scale = 6, updatable = false)
    private BigDecimal observedValue;

    /** Nombre de périodes consécutives au-delà du seuil au moment du déclenchement. */
    @Column(name = "breach_periods", nullable = false, updatable = false)
    private int breachPeriods;

    @Column(nullable = false, length = 255, updatable = false)
    private String title;

    @Column(columnDefinition = "TEXT", updatable = false)
    private String description;

    /**
     * Le critère d'efficacité, rédigé par la règle et non par un utilisateur.
     *
     * <p>Pour un écart d'objectif, l'efficacité EST le retour du KPI sous la
     * cible sur la durée définie. Le laisser saisir reviendrait à permettre de
     * clore sur un critère plus facile que celui qu'on s'était donné.
     */
    @Column(name = "effectiveness_criterion", nullable = false,
            columnDefinition = "TEXT", updatable = false)
    private String effectivenessCriterion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProposalStatus status;

    @Column(name = "capa_case_id")
    private UUID capaCaseId;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = ProposalStatus.PENDING;
    }

    /** {@code true} si le pilote ne l'a pas encore tranchée. */
    public boolean isPending() { return status == ProposalStatus.PENDING; }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getObjectiveId() { return objectiveId; }
    public void setObjectiveId(UUID objectiveId) { this.objectiveId = objectiveId; }
    public ObjectiveLevel getLevel() { return level; }
    public void setLevel(ObjectiveLevel level) { this.level = level; }
    public Instant getPeriodStart() { return periodStart; }
    public void setPeriodStart(Instant periodStart) { this.periodStart = periodStart; }
    public Instant getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(Instant periodEnd) { this.periodEnd = periodEnd; }
    public BigDecimal getObservedValue() { return observedValue; }
    public void setObservedValue(BigDecimal observedValue) { this.observedValue = observedValue; }
    public int getBreachPeriods() { return breachPeriods; }
    public void setBreachPeriods(int breachPeriods) { this.breachPeriods = breachPeriods; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEffectivenessCriterion() { return effectivenessCriterion; }
    public void setEffectivenessCriterion(String c) { this.effectivenessCriterion = c; }
    public ProposalStatus getStatus() { return status; }
    public void setStatus(ProposalStatus status) { this.status = status; }
    public UUID getCapaCaseId() { return capaCaseId; }
    public void setCapaCaseId(UUID capaCaseId) { this.capaCaseId = capaCaseId; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public UUID getDecidedBy() { return decidedBy; }
    public void setDecidedBy(UUID decidedBy) { this.decidedBy = decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant decidedAt) { this.decidedAt = decidedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
