package com.openlab.qualitos.quality.supplier;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fournisseur (§4.6).
 *
 * Le score est mis à jour par {@link SupplierScoringService} — JAMAIS écrit
 * directement depuis l'API. Cela évite les manipulations qui contourneraient
 * la formule ISO 9001 §8.4.
 */
@Entity
@Table(name = "suppliers",
        uniqueConstraints = @UniqueConstraint(name = "uk_supplier_tenant_code",
                columnNames = {"tenant_id", "code"}),
        indexes = {
                @Index(name = "idx_supplier_tenant", columnList = "tenant_id"),
                @Index(name = "idx_supplier_tenant_status", columnList = "tenant_id, status"),
                @Index(name = "idx_supplier_tenant_score", columnList = "tenant_id, score")
        })
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String code;

    @Column(nullable = false, length = 250)
    private String name;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "contact_email", length = 320)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "supplier_type", nullable = false, length = 32)
    private SupplierType supplierType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SupplierStatus status;

    /** Score qualité 0..100. Recalculé par le service, jamais par le client. */
    @Column(nullable = false)
    private int score;

    @Column(name = "last_audit_at")
    private LocalDate lastAuditAt;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = SupplierStatus.PROSPECT;
        if (score == 0) score = 100;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    // getters / setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public SupplierType getSupplierType() { return supplierType; }
    public void setSupplierType(SupplierType supplierType) { this.supplierType = supplierType; }

    public SupplierStatus getStatus() { return status; }
    public void setStatus(SupplierStatus status) { this.status = status; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public LocalDate getLastAuditAt() { return lastAuditAt; }
    public void setLastAuditAt(LocalDate lastAuditAt) { this.lastAuditAt = lastAuditAt; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public UUID getApprovedBy() { return approvedBy; }
    public void setApprovedBy(UUID approvedBy) { this.approvedBy = approvedBy; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
