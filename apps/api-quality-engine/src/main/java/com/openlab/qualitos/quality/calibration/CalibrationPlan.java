package com.openlab.qualitos.quality.calibration;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Plan de calibration : un seul actif par équipement (UNIQUE).
 *
 * Le champ {@link #nextDueOn} est dérivé : il est recalculé lorsqu'un
 * {@link CalibrationRecord} est enregistré ({@code performedOn + frequencyMonths}).
 * Avant le premier record, il vaut la date initiale fournie par l'admin.
 */
@Entity
@Table(name = "calibration_plans",
        uniqueConstraints = @UniqueConstraint(name = "uk_calibration_plan_equipment",
                columnNames = {"equipment_id"}),
        indexes = {
                @Index(name = "idx_calibration_plan_tenant", columnList = "tenant_id"),
                @Index(name = "idx_calibration_plan_due", columnList = "tenant_id, next_due_on")
        })
public class CalibrationPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "equipment_id", nullable = false)
    private UUID equipmentId;

    @Column(name = "frequency_months", nullable = false)
    private int frequencyMonths;

    @Column(name = "procedure_reference", length = 500)
    private String procedureReference;

    @Column(name = "tolerance", length = 500)
    private String tolerance;

    @Column(name = "accreditation_ref", length = 250)
    private String accreditationRef;

    @Column(name = "last_calibrated_on")
    private LocalDate lastCalibratedOn;

    @Column(name = "next_due_on", nullable = false)
    private LocalDate nextDueOn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public boolean isOverdue(LocalDate ref) {
        return nextDueOn != null && nextDueOn.isBefore(ref);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getEquipmentId() { return equipmentId; }
    public void setEquipmentId(UUID equipmentId) { this.equipmentId = equipmentId; }
    public int getFrequencyMonths() { return frequencyMonths; }
    public void setFrequencyMonths(int frequencyMonths) { this.frequencyMonths = frequencyMonths; }
    public String getProcedureReference() { return procedureReference; }
    public void setProcedureReference(String procedureReference) { this.procedureReference = procedureReference; }
    public String getTolerance() { return tolerance; }
    public void setTolerance(String tolerance) { this.tolerance = tolerance; }
    public String getAccreditationRef() { return accreditationRef; }
    public void setAccreditationRef(String accreditationRef) { this.accreditationRef = accreditationRef; }
    public LocalDate getLastCalibratedOn() { return lastCalibratedOn; }
    public void setLastCalibratedOn(LocalDate lastCalibratedOn) { this.lastCalibratedOn = lastCalibratedOn; }
    public LocalDate getNextDueOn() { return nextDueOn; }
    public void setNextDueOn(LocalDate nextDueOn) { this.nextDueOn = nextDueOn; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
