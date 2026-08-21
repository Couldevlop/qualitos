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
    private String sampleSize;
    private String sampleFrequency;
    private String controlMethod;
    private String reactionPlan;
    private UUID fmeaItemId;
    private String sopReference;
    private InputOutput inputOutput;
    private String whoMeasures;
    private String recordingLocation;

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

    /**
     * Les champs facultatifs, posés d'un bloc — ils se saisissent ensemble à
     * l'écran.
     *
     * <p>Rassemblés dans un objet plutôt qu'alignés en dix-sept paramètres : au
     * delà d'une poignée, deux arguments de même type finissent par s'échanger
     * sans que le compilateur ne bronche, et la tolérance basse se retrouve en
     * tolérance haute.
     */
    public record Details(UUID operationId, String machine, String characteristicNo,
                          CharacteristicClass specialClass, String specification,
                          BigDecimal toleranceLower, BigDecimal toleranceUpper, String unit,
                          String measurementTechnique, String sampleSize, String sampleFrequency,
                          String controlMethod, String reactionPlan,
                          String sopReference, InputOutput inputOutput,
                          String whoMeasures, String recordingLocation) {
    }

    public void describe(Details details) {
        Objects.requireNonNull(details, "details");
        // Les longueurs sont vérifiées ICI et pas seulement à la frontière HTTP.
        // Le moteur de propositions de révision écrit des lignes sans passer par
        // le contrôleur : sa validation ne le protège pas. Sans cette garde, une
        // valeur trop longue partait jusqu'à la base et revenait en erreur
        // d'intégrité — un 500 là où l'appelant méritait un refus nommé.
        this.operationId = details.operationId();
        this.machine = details.machine();
        this.characteristicNo = details.characteristicNo();
        this.specialClass = details.specialClass() == null
                ? CharacteristicClass.STANDARD : details.specialClass();
        this.specification = details.specification();
        this.toleranceLower = details.toleranceLower();
        this.toleranceUpper = details.toleranceUpper();
        this.unit = details.unit();
        this.measurementTechnique = bounded(details.measurementTechnique(), 250, "measurementTechnique");
        this.sampleFrequency = bounded(details.sampleFrequency(), 120, "sampleFrequency");
        this.controlMethod = details.controlMethod();
        this.reactionPlan = details.reactionPlan();
        this.sampleSize = bounded(details.sampleSize(), 120, "sampleSize");
        this.sopReference = bounded(details.sopReference(), 64, "sopReference");
        this.inputOutput = details.inputOutput();
        this.whoMeasures = bounded(details.whoMeasures(), 250, "whoMeasures");
        this.recordingLocation = bounded(details.recordingLocation(), 250, "recordingLocation");
    }

    /**
     * Refuse une valeur plus longue que la colonne qui l'accueillera. Tronquer
     * silencieusement serait pire : un plan de réaction coupé au milieu se lit
     * comme une consigne complète.
     */
    private static String bounded(String value, int max, String field) {
        if (value != null && value.length() > max) {
            throw new IllegalArgumentException(
                    "Control plan field '" + field + "' exceeds " + max + " characters");
        }
        return value;
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
    public String getSampleSize() { return sampleSize; }
    public String getSampleFrequency() { return sampleFrequency; }
    public String getControlMethod() { return controlMethod; }
    public String getReactionPlan() { return reactionPlan; }
    public UUID getFmeaItemId() { return fmeaItemId; }
    public String getSopReference() { return sopReference; }
    public InputOutput getInputOutput() { return inputOutput; }
    public String getWhoMeasures() { return whoMeasures; }
    public String getRecordingLocation() { return recordingLocation; }
}
