package com.openlab.qualitos.quality.industry;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Activation d'un Industry Pack pour un tenant donné.
 *
 * Un même couple (tenant, pack_code) ne peut exister qu'une seule fois : on garde
 * l'historique en mettant {@link ActivationStatus#DEACTIVATED} et {@code deactivatedAt}
 * plutôt qu'en supprimant la ligne — l'audit ISO 19011 exige une trace.
 *
 * Une réactivation crée une NOUVELLE ligne (deactivatedAt préservé sur l'ancienne)
 * pour ne pas effacer l'historique. La contrainte UNIQUE est donc
 * (tenant_id, pack_code, status='ACTIVE') et non pas globale — implémentée via
 * index partiel PostgreSQL.
 */
@Entity
@Table(name = "tenant_industry_pack_activations",
        indexes = {
                @Index(name = "idx_tipa_tenant", columnList = "tenant_id"),
                @Index(name = "idx_tipa_tenant_pack", columnList = "tenant_id, pack_code")
        })
public class TenantIndustryPackActivation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "pack_code", nullable = false, length = 64)
    private String packCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ActivationStatus status;

    @Column(name = "activated_by", nullable = false)
    private UUID activatedBy;

    @Column(name = "activated_at", nullable = false, updatable = false)
    private Instant activatedAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "deactivated_by")
    private UUID deactivatedBy;

    /** JSON optionnel pour overrides par-tenant (KPIs custom, glossaire, etc.). */
    // TEXT cote DB (V17) — éviter le mapping @Lob→oid (cf. AuditEvent.payloadJson).
    @Column(name = "config_overrides_json", columnDefinition = "TEXT")
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.LONGVARCHAR)
    private String configOverridesJson;

    @PrePersist
    void prePersist() {
        if (activatedAt == null) activatedAt = Instant.now();
        if (status == null) status = ActivationStatus.ACTIVE;
    }

    // getters / setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }

    public String getPackCode() { return packCode; }
    public void setPackCode(String packCode) { this.packCode = packCode; }

    public ActivationStatus getStatus() { return status; }
    public void setStatus(ActivationStatus status) { this.status = status; }

    public UUID getActivatedBy() { return activatedBy; }
    public void setActivatedBy(UUID activatedBy) { this.activatedBy = activatedBy; }

    public Instant getActivatedAt() { return activatedAt; }
    public void setActivatedAt(Instant activatedAt) { this.activatedAt = activatedAt; }

    public Instant getDeactivatedAt() { return deactivatedAt; }
    public void setDeactivatedAt(Instant deactivatedAt) { this.deactivatedAt = deactivatedAt; }

    public UUID getDeactivatedBy() { return deactivatedBy; }
    public void setDeactivatedBy(UUID deactivatedBy) { this.deactivatedBy = deactivatedBy; }

    public String getConfigOverridesJson() { return configOverridesJson; }
    public void setConfigOverridesJson(String configOverridesJson) { this.configOverridesJson = configOverridesJson; }
}
