package com.openlab.qualitos.quality.standards;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Lien entre une exigence normative et un élément du tenant qui sert de preuve
 * (document, audit, CAPA, formation, etc.).
 *
 * L'unicité (tenant_standard_id, requirement_id, evidence_ref_id) garantit
 * qu'on ne lie pas deux fois la même preuve à la même exigence.
 */
@Entity
@Table(name = "requirement_evidences",
        uniqueConstraints = @UniqueConstraint(name = "uk_evidence_ts_req_ref",
                columnNames = {"tenant_standard_id", "requirement_id", "evidence_ref_id"}))
@Getter
@Setter
@NoArgsConstructor
public class RequirementEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_standard_id", nullable = false, updatable = false)
    private TenantStandard tenantStandard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requirement_id", nullable = false, updatable = false)
    private StandardRequirement requirement;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 30)
    private EvidenceType evidenceType;

    /** UUID de l'élément référencé (Document, AuditPlan, CapaCase, etc.). */
    @Column(name = "evidence_ref_id")
    private UUID evidenceRefId;

    /** URI externe si EXTERNAL_FILE / OTHER. */
    @Column(name = "evidence_uri", length = 1024)
    private String evidenceUri;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "linked_by", nullable = false)
    private UUID linkedBy;

    @Column(name = "linked_at", nullable = false, updatable = false)
    private Instant linkedAt;

    @PrePersist
    void prePersist() {
        if (this.linkedAt == null) {
            this.linkedAt = Instant.now();
        }
    }
}
