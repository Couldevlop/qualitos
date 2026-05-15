package com.openlab.qualitos.quality.calibration;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "calibration_records",
        indexes = {
                @Index(name = "idx_calibration_record_equipment",
                        columnList = "equipment_id, performed_on"),
                @Index(name = "idx_calibration_record_tenant",
                        columnList = "tenant_id, performed_on")
        })
public class CalibrationRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "equipment_id", nullable = false)
    private UUID equipmentId;

    @Column(name = "performed_on", nullable = false)
    private LocalDate performedOn;

    @Column(name = "performed_by_user_id")
    private UUID performedByUserId;

    @Column(name = "performed_by_org", length = 250)
    private String performedByOrg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private CalibrationResult result;

    @Column(name = "measurements", length = 4000)
    private String measurements;

    @Column(name = "certificate_reference", length = 250)
    private String certificateReference;

    @Column(name = "next_due_override")
    private LocalDate nextDueOverride;

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
    public UUID getEquipmentId() { return equipmentId; }
    public void setEquipmentId(UUID equipmentId) { this.equipmentId = equipmentId; }
    public LocalDate getPerformedOn() { return performedOn; }
    public void setPerformedOn(LocalDate performedOn) { this.performedOn = performedOn; }
    public UUID getPerformedByUserId() { return performedByUserId; }
    public void setPerformedByUserId(UUID performedByUserId) { this.performedByUserId = performedByUserId; }
    public String getPerformedByOrg() { return performedByOrg; }
    public void setPerformedByOrg(String performedByOrg) { this.performedByOrg = performedByOrg; }
    public CalibrationResult getResult() { return result; }
    public void setResult(CalibrationResult result) { this.result = result; }
    public String getMeasurements() { return measurements; }
    public void setMeasurements(String measurements) { this.measurements = measurements; }
    public String getCertificateReference() { return certificateReference; }
    public void setCertificateReference(String certificateReference) { this.certificateReference = certificateReference; }
    public LocalDate getNextDueOverride() { return nextDueOverride; }
    public void setNextDueOverride(LocalDate nextDueOverride) { this.nextDueOverride = nextDueOverride; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
