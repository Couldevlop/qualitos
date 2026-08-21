package com.openlab.qualitos.quality.controlplan.infrastructure;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "control_plan_lines",
        indexes = {
                @Index(name = "idx_control_plan_lines_plan", columnList = "plan_id, sequence_no"),
                @Index(name = "idx_control_plan_lines_fmea", columnList = "fmea_item_id")
        })
public class ControlPlanLineJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private UUID planId;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    @Column(name = "operation_id")
    private UUID operationId;

    @Column(length = 250)
    private String machine;

    @Column(name = "characteristic_no", length = 32)
    private String characteristicNo;

    @Column(name = "characteristic_label", nullable = false, length = 500)
    private String characteristicLabel;

    @Column(name = "characteristic_type", nullable = false, length = 20)
    private String characteristicType;

    @Column(name = "special_class", length = 20)
    private String specialClass;

    @Column(length = 500)
    private String specification;

    @Column(name = "tolerance_lower", precision = 18, scale = 6)
    private BigDecimal toleranceLower;

    @Column(name = "tolerance_upper", precision = 18, scale = 6)
    private BigDecimal toleranceUpper;

    @Column(length = 24)
    private String unit;

    @Column(name = "measurement_technique", length = 250)
    private String measurementTechnique;

    @Column(name = "sample_size")
    private Integer sampleSize;

    @Column(name = "sample_frequency", length = 120)
    private String sampleFrequency;

    @Column(name = "control_method", length = 500)
    private String controlMethod;

    @Column(name = "reaction_plan", length = 1000)
    private String reactionPlan;

    @Column(name = "fmea_item_id")
    private UUID fmeaItemId;

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getPlanId() { return planId; }
    public void setPlanId(UUID v) { this.planId = v; }
    public int getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(int v) { this.sequenceNo = v; }
    public UUID getOperationId() { return operationId; }
    public void setOperationId(UUID v) { this.operationId = v; }
    public String getMachine() { return machine; }
    public void setMachine(String v) { this.machine = v; }
    public String getCharacteristicNo() { return characteristicNo; }
    public void setCharacteristicNo(String v) { this.characteristicNo = v; }
    public String getCharacteristicLabel() { return characteristicLabel; }
    public void setCharacteristicLabel(String v) { this.characteristicLabel = v; }
    public String getCharacteristicType() { return characteristicType; }
    public void setCharacteristicType(String v) { this.characteristicType = v; }
    public String getSpecialClass() { return specialClass; }
    public void setSpecialClass(String v) { this.specialClass = v; }
    public String getSpecification() { return specification; }
    public void setSpecification(String v) { this.specification = v; }
    public BigDecimal getToleranceLower() { return toleranceLower; }
    public void setToleranceLower(BigDecimal v) { this.toleranceLower = v; }
    public BigDecimal getToleranceUpper() { return toleranceUpper; }
    public void setToleranceUpper(BigDecimal v) { this.toleranceUpper = v; }
    public String getUnit() { return unit; }
    public void setUnit(String v) { this.unit = v; }
    public String getMeasurementTechnique() { return measurementTechnique; }
    public void setMeasurementTechnique(String v) { this.measurementTechnique = v; }
    public Integer getSampleSize() { return sampleSize; }
    public void setSampleSize(Integer v) { this.sampleSize = v; }
    public String getSampleFrequency() { return sampleFrequency; }
    public void setSampleFrequency(String v) { this.sampleFrequency = v; }
    public String getControlMethod() { return controlMethod; }
    public void setControlMethod(String v) { this.controlMethod = v; }
    public String getReactionPlan() { return reactionPlan; }
    public void setReactionPlan(String v) { this.reactionPlan = v; }
    public UUID getFmeaItemId() { return fmeaItemId; }
    public void setFmeaItemId(UUID v) { this.fmeaItemId = v; }
}
