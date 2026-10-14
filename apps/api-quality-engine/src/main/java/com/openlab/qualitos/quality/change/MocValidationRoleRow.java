package com.openlab.qualitos.quality.change;

import jakarta.persistence.*;

import java.util.UUID;

/** Un rôle de validation du client, et les classifications qui l'exigent. */
@Entity
@Table(name = "moc_validation_roles",
        indexes = @Index(name = "ix_moc_validation_roles_tenant", columnList = "tenant_id, ordinal"))
public class MocValidationRoleRow {

    @Id
    @Column(nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false)
    private int ordinal;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "required_minor", nullable = false)
    private boolean requiredMinor;

    @Column(name = "required_major", nullable = false)
    private boolean requiredMajor;

    @Column(name = "required_critical", nullable = false)
    private boolean requiredCritical;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public int getOrdinal() { return ordinal; }
    public void setOrdinal(int ordinal) { this.ordinal = ordinal; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public boolean isRequiredMinor() { return requiredMinor; }
    public void setRequiredMinor(boolean requiredMinor) { this.requiredMinor = requiredMinor; }
    public boolean isRequiredMajor() { return requiredMajor; }
    public void setRequiredMajor(boolean requiredMajor) { this.requiredMajor = requiredMajor; }
    public boolean isRequiredCritical() { return requiredCritical; }
    public void setRequiredCritical(boolean requiredCritical) { this.requiredCritical = requiredCritical; }
}
