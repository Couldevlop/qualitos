package com.openlab.qualitos.quality.controlplan.domain;

import com.openlab.qualitos.quality.risk.CharacteristicClass;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Une ligne du plan, au format AIAG : quoi surveiller, où, comment, à quelle
 * fréquence, et que faire quand la mesure sort des limites.
 *
 * <p>Seuls la caractéristique et son type sont obligatoires. Un plan se remplit
 * par passes successives — d'abord la liste des caractéristiques, ensuite les
 * moyens de mesure, ensuite les fréquences — et refuser une ligne incomplète
 * empêcherait simplement de travailler.
 */
public final class ControlPlanLine {

    private UUID id;
    private final UUID tenantId;
    private final UUID planId;
    private int sequenceNo;
    private UUID operationId;
    private String machine;
    private String characteristicNo;
    private String characteristicLabel;
    private CharacteristicType characteristicType;
    private CharacteristicClass specialClass;
    private String specification;
    private BigDecimal toleranceLower;
    private BigDecimal toleranceUpper;
    private String unit;
    private String measurementTechnique;
    private Integer sampleSize;
    private String sampleFrequency;
    private String controlMethod;
    private String reactionPlan;
    private UUID fmeaItemId;

    private ControlPlanLine(UUID tenantId, UUID planId, int sequenceNo,
                            String characteristicLabel, CharacteristicType characteristicType) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.planId = Objects.requireNonNull(planId, "planId");
        this.sequenceNo = sequenceNo;
        this.characteristicLabel = requireLabel(characteristicLabel);
        this.characteristicType = Objects.requireNonNull(characteristicType, "characteristicType");
        this.specialClass = CharacteristicClass.STANDARD;
    }

    public static ControlPlanLine create(UUID tenantId, UUID planId, int sequenceNo,
                                         String characteristicLabel, CharacteristicType type) {
        return new ControlPlanLine(tenantId, planId, sequenceNo, characteristicLabel, type);
    }

    /** Reconstruction depuis la persistance, et recopie d'une ligne dans une révision. */
    public static ControlPlanLine rehydrate(UUID id, UUID tenantId, UUID planId, int sequenceNo,
                                            String characteristicLabel, CharacteristicType type) {
        ControlPlanLine line = new ControlPlanLine(tenantId, planId, sequenceNo, characteristicLabel, type);
        line.id = id;
        return line;
    }

    private static String requireLabel(String label) {
        String trimmed = label == null ? "" : label.trim();
        if (trimmed.isEmpty() || trimmed.length() > 500) {
            throw new IllegalArgumentException("Invalid control plan characteristic label");
        }
        return trimmed;
    }

    /** Les champs facultatifs, posés d'un bloc — ils se saisissent ensemble à l'écran. */
    public void describe(UUID operationId, String machine, String characteristicNo,
                         CharacteristicClass specialClass, String specification,
                         BigDecimal toleranceLower, BigDecimal toleranceUpper, String unit,
                         String measurementTechnique, Integer sampleSize, String sampleFrequency,
                         String controlMethod, String reactionPlan) {
        this.operationId = operationId;
        this.machine = machine;
        this.characteristicNo = characteristicNo;
        this.specialClass = specialClass == null ? CharacteristicClass.STANDARD : specialClass;
        this.specification = specification;
        this.toleranceLower = toleranceLower;
        this.toleranceUpper = toleranceUpper;
        this.unit = unit;
        this.measurementTechnique = measurementTechnique;
        this.sampleSize = sampleSize;
        this.sampleFrequency = sampleFrequency;
        this.controlMethod = controlMethod;
        this.reactionPlan = reactionPlan;
    }

    public void rename(String characteristicLabel, CharacteristicType type, int sequenceNo) {
        this.characteristicLabel = requireLabel(characteristicLabel);
        this.characteristicType = Objects.requireNonNull(type, "characteristicType");
        this.sequenceNo = sequenceNo;
    }

    /** Le lien qui dit POURQUOI ce contrôle existe. */
    public void justifiedBy(UUID fmeaItemId) { this.fmeaItemId = fmeaItemId; }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getPlanId() { return planId; }
    public int getSequenceNo() { return sequenceNo; }
    public UUID getOperationId() { return operationId; }
    public String getMachine() { return machine; }
    public String getCharacteristicNo() { return characteristicNo; }
    public String getCharacteristicLabel() { return characteristicLabel; }
    public CharacteristicType getCharacteristicType() { return characteristicType; }
    public CharacteristicClass getSpecialClass() { return specialClass; }
    public String getSpecification() { return specification; }
    public BigDecimal getToleranceLower() { return toleranceLower; }
    public BigDecimal getToleranceUpper() { return toleranceUpper; }
    public String getUnit() { return unit; }
    public String getMeasurementTechnique() { return measurementTechnique; }
    public Integer getSampleSize() { return sampleSize; }
    public String getSampleFrequency() { return sampleFrequency; }
    public String getControlMethod() { return controlMethod; }
    public String getReactionPlan() { return reactionPlan; }
    public UUID getFmeaItemId() { return fmeaItemId; }
}
