package com.openlab.qualitos.quality.supplier;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Enregistrement d'audit fournisseur — version allégée du module Audit complet.
 * Pour un audit ISO 19011 formel avec checklists, voir le module audit.
 */
@Entity
@Table(name = "supplier_audit_records",
        indexes = {
                @Index(name = "idx_supplier_audit_supplier",
                        columnList = "supplier_id, audited_on"),
                @Index(name = "idx_supplier_audit_tenant",
                        columnList = "tenant_id, audited_on")
        })
public class SupplierAuditRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "audited_on", nullable = false)
    private LocalDate auditedOn;

    /** Score audit 0..100. */
    @Column(nullable = false)
    private int score;

    @Column(name = "auditor_user_id")
    private UUID auditorUserId;

    @Column(name = "findings_summary", length = 2000)
    private String findingsSummary;

    @Column(name = "critical_findings_count", nullable = false)
    private int criticalFindingsCount;

    @Column(name = "major_findings_count", nullable = false)
    private int majorFindingsCount;

    @Column(name = "minor_findings_count", nullable = false)
    private int minorFindingsCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
    public LocalDate getAuditedOn() { return auditedOn; }
    public void setAuditedOn(LocalDate auditedOn) { this.auditedOn = auditedOn; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public UUID getAuditorUserId() { return auditorUserId; }
    public void setAuditorUserId(UUID auditorUserId) { this.auditorUserId = auditorUserId; }
    public String getFindingsSummary() { return findingsSummary; }
    public void setFindingsSummary(String findingsSummary) { this.findingsSummary = findingsSummary; }
    public int getCriticalFindingsCount() { return criticalFindingsCount; }
    public void setCriticalFindingsCount(int criticalFindingsCount) { this.criticalFindingsCount = criticalFindingsCount; }
    public int getMajorFindingsCount() { return majorFindingsCount; }
    public void setMajorFindingsCount(int majorFindingsCount) { this.majorFindingsCount = majorFindingsCount; }
    public int getMinorFindingsCount() { return minorFindingsCount; }
    public void setMinorFindingsCount(int minorFindingsCount) { this.minorFindingsCount = minorFindingsCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
