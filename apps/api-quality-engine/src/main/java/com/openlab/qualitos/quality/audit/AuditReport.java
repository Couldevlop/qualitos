package com.openlab.qualitos.quality.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Rapport d'un audit (§4.4, ISO 19011 §6.5) : ce qui se remet à la direction ou à
 * l'organisme une fois l'audit clos.
 *
 * <p>La synthèse rédigée n'est pas portée ici mais par {@link AuditPlan#getReportSummary()},
 * où l'écrivent déjà la génération par IA et la clôture. À l'émission, elle est
 * recopiée dans {@link #issuedSummary} avec les chiffres clés : le rapport émis se
 * relit tel qu'il a été remis, même si un constat est retouché ensuite.
 */
@Entity
@Table(name = "audit_reports")
@Getter
@Setter
@NoArgsConstructor
public class AuditReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AuditReportStatus status = AuditReportStatus.DRAFT;

    /** Points forts relevés. */
    @Column(columnDefinition = "TEXT")
    private String strengths;

    /** Axes d'amélioration. */
    @Column(columnDefinition = "TEXT")
    private String improvements;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private AuditConclusion conclusion;

    // --- instantané figé à l'émission ---

    @Column(name = "major_nc_count")
    private Integer majorNcCount;

    @Column(name = "minor_nc_count")
    private Integer minorNcCount;

    @Column(name = "observation_count")
    private Integer observationCount;

    @Column(name = "opportunity_count")
    private Integer opportunityCount;

    @Column(name = "conformity_count")
    private Integer conformityCount;

    @Column(name = "conformity_score")
    private Double conformityScore;

    @Column(name = "issued_summary", columnDefinition = "TEXT")
    private String issuedSummary;

    /** Empreinte du contenu émis — c'est elle que le journal chaîné ancre. */
    @Column(name = "content_sha256", length = 64)
    private String contentSha256;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "issued_by")
    private UUID issuedBy;

    @Column(name = "issued_by_name", length = 255)
    private String issuedByName;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Deux émissions concurrentes : la seconde échoue au lieu d'écraser la première. */
    @Version
    private long version;

    public boolean isIssued() {
        return status == AuditReportStatus.ISSUED;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
