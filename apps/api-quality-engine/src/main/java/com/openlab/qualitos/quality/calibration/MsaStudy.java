package com.openlab.qualitos.quality.calibration;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "calibration_msa_studies",
        indexes = {
                @Index(name = "idx_msa_equipment", columnList = "equipment_id, performed_on"),
                @Index(name = "idx_msa_tenant_type", columnList = "tenant_id, type")
        })
public class MsaStudy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "equipment_id", nullable = false)
    private UUID equipmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MsaType type;

    @Column(name = "performed_on", nullable = false)
    private LocalDate performedOn;

    /** Valeur étudiée — selon le type : %R&R, biais (unité), pente, etc. */
    @Column(name = "study_value", precision = 12, scale = 4, nullable = false)
    private BigDecimal studyValue;

    @Column(name = "passing_threshold", precision = 12, scale = 4)
    private BigDecimal passingThreshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MsaResult result;

    @Column(length = 2000)
    private String notes;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

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
    public MsaType getType() { return type; }
    public void setType(MsaType type) { this.type = type; }
    public LocalDate getPerformedOn() { return performedOn; }
    public void setPerformedOn(LocalDate performedOn) { this.performedOn = performedOn; }
    public BigDecimal getStudyValue() { return studyValue; }
    public void setStudyValue(BigDecimal studyValue) { this.studyValue = studyValue; }
    public BigDecimal getPassingThreshold() { return passingThreshold; }
    public void setPassingThreshold(BigDecimal passingThreshold) { this.passingThreshold = passingThreshold; }
    public MsaResult getResult() { return result; }
    public void setResult(MsaResult result) { this.result = result; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
