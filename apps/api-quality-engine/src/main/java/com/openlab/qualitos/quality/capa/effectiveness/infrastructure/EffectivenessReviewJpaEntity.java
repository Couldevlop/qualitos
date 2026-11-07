package com.openlab.qualitos.quality.capa.effectiveness.infrastructure;

import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Ligne de {@code capa_effectiveness_reviews} (ADR 0108).
 *
 * <p>Sans {@code @Setter} global ni {@code @UpdateTimestamp} : la table est
 * append-only, et le laisser croire modifiable inviterait un jour à l'écrire.
 * Un jugement qu'on peut réécrire ne prouve rien, et c'est exactement ce qu'un
 * auditeur vient contrôler.
 *
 * <p>Pas de relation JPA vers {@code CapaCase}, seulement sa clé. La contrainte
 * de clé étrangère est posée en base ; une association ici ferait charger le
 * dossier entier — et ses actions — pour lire une ligne d'historique.
 */
@Entity
@Table(name = "capa_effectiveness_reviews")
public class EffectivenessReviewJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "capa_case_id", nullable = false, updatable = false)
    private UUID capaCaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 20, updatable = false)
    private EffectivenessVerdict verdict;

    @Column(name = "criterion", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String criterion;

    @Column(name = "baseline", length = 120, updatable = false)
    private String baseline;

    @Column(name = "target", length = 120, updatable = false)
    private String target;

    @Column(name = "observed", nullable = false, length = 120, updatable = false)
    private String observed;

    @Column(name = "comment", columnDefinition = "TEXT", updatable = false)
    private String comment;

    @Column(name = "reviewer_id", updatable = false)
    private UUID reviewerId;

    @Column(name = "reviewer_name", length = 255, updatable = false)
    private String reviewerName;

    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private Instant reviewedAt;

    /** Requis par JPA. */
    protected EffectivenessReviewJpaEntity() {
    }

    EffectivenessReviewJpaEntity(UUID id, UUID tenantId, UUID capaCaseId,
                                 EffectivenessVerdict verdict, String criterion,
                                 String baseline, String target, String observed,
                                 String comment, UUID reviewerId, String reviewerName,
                                 Instant reviewedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.capaCaseId = capaCaseId;
        this.verdict = verdict;
        this.criterion = criterion;
        this.baseline = baseline;
        this.target = target;
        this.observed = observed;
        this.comment = comment;
        this.reviewerId = reviewerId;
        this.reviewerName = reviewerName;
        this.reviewedAt = reviewedAt;
    }

    UUID getId() {
        return id;
    }

    UUID getTenantId() {
        return tenantId;
    }

    UUID getCapaCaseId() {
        return capaCaseId;
    }

    EffectivenessVerdict getVerdict() {
        return verdict;
    }

    String getCriterion() {
        return criterion;
    }

    String getBaseline() {
        return baseline;
    }

    String getTarget() {
        return target;
    }

    String getObserved() {
        return observed;
    }

    String getComment() {
        return comment;
    }

    UUID getReviewerId() {
        return reviewerId;
    }

    String getReviewerName() {
        return reviewerName;
    }

    Instant getReviewedAt() {
        return reviewedAt;
    }
}
