package com.openlab.qualitos.quality.change;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Les seuils de classification et le délai de rappel d'un client. Sans ligne, les valeurs livrées (25 / 50, 3 jours). */
@Entity
@Table(name = "moc_settings")
public class MocSettingsRow {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "major_threshold", nullable = false)
    private int majorThreshold;

    @Column(name = "critical_threshold", nullable = false)
    private int criticalThreshold;

    @Column(name = "reminder_days", nullable = false)
    private int reminderDays;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public int getMajorThreshold() { return majorThreshold; }
    public void setMajorThreshold(int majorThreshold) { this.majorThreshold = majorThreshold; }
    public int getCriticalThreshold() { return criticalThreshold; }
    public void setCriticalThreshold(int criticalThreshold) { this.criticalThreshold = criticalThreshold; }
    public int getReminderDays() { return reminderDays; }
    public void setReminderDays(int reminderDays) { this.reminderDays = reminderDays; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
