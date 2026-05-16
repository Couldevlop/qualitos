package com.openlab.qualitos.quality.risk;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Projet FMEA (Process/Design/System/Service/Bow-tie).
 *
 * Le couple (tenant_id, code) est unique : un projet a un identifiant humain stable.
 * La révision est incrémentée à chaque sortie de DRAFT, pour matérialiser
 * l'historique des revues exigé par IATF 16949 et AS9100.
 */
@Entity
@Table(name = "fmea_projects",
        uniqueConstraints = @UniqueConstraint(name = "uk_fmea_tenant_code",
                columnNames = {"tenant_id", "code"}),
        indexes = {
                @Index(name = "idx_fmea_tenant", columnList = "tenant_id"),
                @Index(name = "idx_fmea_tenant_status", columnList = "tenant_id, status")
        })
public class FmeaProject {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String code;

    @Column(nullable = false, length = 250)
    private String name;

    @Column(length = 1000)
    private String scope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FmeaType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private FmeaStatus status;

    /** Seuil RPN à partir duquel un item est dit "critique". Défaut 100. */
    @Column(name = "critical_rpn_threshold", nullable = false)
    private int criticalRpnThreshold;

    @Column(nullable = false)
    private int revision;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

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
        if (status == null) status = FmeaStatus.DRAFT;
        if (criticalRpnThreshold == 0) criticalRpnThreshold = 100;
        if (revision == 0) revision = 1;
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

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public FmeaType getType() { return type; }
    public void setType(FmeaType type) { this.type = type; }

    public FmeaStatus getStatus() { return status; }
    public void setStatus(FmeaStatus status) { this.status = status; }

    public int getCriticalRpnThreshold() { return criticalRpnThreshold; }
    public void setCriticalRpnThreshold(int criticalRpnThreshold) { this.criticalRpnThreshold = criticalRpnThreshold; }

    public int getRevision() { return revision; }
    public void setRevision(int revision) { this.revision = revision; }

    public UUID getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(UUID ownerUserId) { this.ownerUserId = ownerUserId; }

    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(Instant lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
