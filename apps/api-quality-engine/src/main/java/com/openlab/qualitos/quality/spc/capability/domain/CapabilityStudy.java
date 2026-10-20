package com.openlab.qualitos.quality.spc.capability.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Une étude de capabilité procédé (Cp / Cpk) sur cartes X̄ – R.
 *
 * <p><b>Cp et Cpk se calculent ici, à chaque écriture.</b> L'écran les calcule
 * aussi, pour suivre la saisie en direct, mais ce qui est enregistré — et ce
 * que lit le registre — vient du serveur : un Cpk présenté en revue ne doit pas
 * dépendre du navigateur qui l'a saisi.
 *
 * <p><b>Les seuils sont figés à la création.</b> L'étude garde les seuils du
 * client en vigueur le jour où elle a été enregistrée, et ses corrections ne
 * les changent pas : relever le seuil de maîtrise ne réécrit pas le verdict
 * des études passées.
 */
public final class CapabilityStudy {

    public static final int PART_REFERENCE_MAX = 120;
    public static final int CHARACTERISTIC_MAX = 150;
    public static final int SPECIFICATION_MAX = 150;
    public static final int MACHINE_MAX = 80;
    public static final int LEADER_NAME_MAX = 150;
    /** Au-delà, une mesure est une faute de frappe, pas une grandeur physique. */
    static final double MEASUREMENT_MAX = 1e12;

    private final UUID id;
    private final UUID tenantId;
    private String partReference;
    private String characteristic;
    private String specification;
    private String machine;
    private UUID leaderUserId;
    private String leaderName;
    private LocalDate studyDate;
    private boolean actionPlan;
    private double lsl;
    private double usl;
    private int subgroupSize;
    private List<List<Double>> measurements;
    private double cp;
    private double cpk;
    private final CapabilityThresholds thresholds;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    @SuppressWarnings("java:S107") // reconstitution depuis la persistance : un champ par colonne
    public CapabilityStudy(UUID id, UUID tenantId, String partReference, String characteristic,
                           String specification, String machine, UUID leaderUserId, String leaderName,
                           LocalDate studyDate, boolean actionPlan, double lsl, double usl, int subgroupSize,
                           List<List<Double>> measurements, double cp, double cpk,
                           CapabilityThresholds thresholds, UUID createdBy, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.partReference = partReference;
        this.characteristic = characteristic;
        this.specification = specification;
        this.machine = machine;
        this.leaderUserId = leaderUserId;
        this.leaderName = leaderName;
        this.studyDate = studyDate;
        this.actionPlan = actionPlan;
        this.lsl = lsl;
        this.usl = usl;
        this.subgroupSize = subgroupSize;
        this.measurements = copie(measurements);
        this.cp = cp;
        this.cpk = cpk;
        this.thresholds = thresholds;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Une étude neuve, validée, sous les seuils en vigueur du client. */
    public static CapabilityStudy record(UUID tenantId, CapabilityStudyDetails details,
                                         CapabilityThresholds thresholds, UUID createdBy, Instant now) {
        Objects.requireNonNull(tenantId, "tenantId");
        Objects.requireNonNull(thresholds, "thresholds");
        CapabilityStudy etude = new CapabilityStudy(null, tenantId, null, null, null, null, null, null,
                null, false, 0, 0, SpcConstants.MIN_SUBGROUP_SIZE, List.of(), 0, 0, thresholds,
                createdBy, now, now);
        etude.apply(details, now);
        return etude;
    }

    /** Corrige l'étude : identification et grille, revalidées et recalculées d'un bloc. */
    public void revise(CapabilityStudyDetails details, Instant now) {
        apply(details, now);
    }

    /** Coche ou décoche le plan d'actions, depuis le registre, sans rouvrir l'étude. */
    public void setActionPlan(boolean actionPlan, Instant now) {
        this.actionPlan = actionPlan;
        this.updatedAt = now;
    }

    /** Le calcul complet, refait depuis la grille enregistrée. */
    public CapabilityCalculation calculation() {
        return CapabilityCalculation.of(lsl, usl, subgroupSize, measurements);
    }

    public CapabilityStatus status() {
        return thresholds.classify(cpk);
    }

    private void apply(CapabilityStudyDetails d, Instant now) {
        if (d == null) {
            throw new SpcValidationException("partReference", "La référence de la pièce est obligatoire.");
        }
        String reference = requireText("partReference", d.partReference(), PART_REFERENCE_MAX,
                "La référence de la pièce est obligatoire.");
        String caracteristique = optionalText("characteristic", d.characteristic(), CHARACTERISTIC_MAX);
        String specif = optionalText("specification", d.specification(), SPECIFICATION_MAX);
        String numMachine = optionalText("machine", d.machine(), MACHINE_MAX);
        String meneur = optionalText("leaderName", d.leaderName(), LEADER_NAME_MAX);
        if (d.leaderUserId() != null && meneur == null) {
            throw new SpcValidationException("leaderName", "Le nom du meneur de l'étude est obligatoire.");
        }
        if (d.studyDate() == null) {
            throw new SpcValidationException("studyDate", "La date de l'étude est obligatoire.");
        }
        double basse = requireLimit("lsl", d.lsl(), "La limite inférieure (LSL) est obligatoire.");
        double haute = requireLimit("usl", d.usl(), "La limite supérieure (USL) est obligatoire.");
        if (haute <= basse) {
            throw new SpcValidationException("usl", "La limite supérieure doit dépasser la limite inférieure.");
        }
        if (d.subgroupSize() == null) {
            throw new SpcValidationException("subgroupSize", "La taille du sous-groupe est obligatoire.");
        }
        int n = d.subgroupSize();
        SpcConstants.forSubgroupSize(n);
        List<List<Double>> grille = requireGrid(d.measurements(), n);

        CapabilityCalculation calcul = CapabilityCalculation.of(basse, haute, n, grille);
        if (calcul.completeSubgroups() == 0) {
            throw new SpcValidationException("measurements",
                    "Saisissez au moins un sous-groupe complet.");
        }
        if (!calcul.computable()) {
            throw new SpcValidationException("measurements",
                    "Les mesures n'ont aucune dispersion : Cp et Cpk ne se calculent pas.");
        }

        this.partReference = reference;
        this.characteristic = caracteristique;
        this.specification = specif;
        this.machine = numMachine;
        this.leaderUserId = d.leaderUserId();
        this.leaderName = meneur;
        this.studyDate = d.studyDate();
        this.actionPlan = d.actionPlan();
        this.lsl = basse;
        this.usl = haute;
        this.subgroupSize = n;
        this.measurements = grille;
        this.cp = arrondi(calcul.cp());
        this.cpk = arrondi(calcul.cpk());
        this.updatedAt = now;
    }

    private static List<List<Double>> requireGrid(List<List<Double>> grille, int n) {
        if (grille == null || grille.size() != n) {
            throw new SpcValidationException("measurements",
                    "La grille doit compter une ligne par échantillon du sous-groupe (" + n + ").");
        }
        for (List<Double> ligne : grille) {
            if (ligne == null || ligne.size() != CapabilityCalculation.SUBGROUPS) {
                throw new SpcValidationException("measurements",
                        "Chaque ligne compte " + CapabilityCalculation.SUBGROUPS + " sous-groupes.");
            }
            for (Double v : ligne) {
                if (v != null && (!Double.isFinite(v) || Math.abs(v) > MEASUREMENT_MAX)) {
                    throw new SpcValidationException("measurements", "Une mesure est hors de la plage admise.");
                }
            }
        }
        return copie(grille);
    }

    private static double requireLimit(String field, Double value, String missing) {
        if (value == null) {
            throw new SpcValidationException(field, missing);
        }
        if (!Double.isFinite(value) || Math.abs(value) > MEASUREMENT_MAX) {
            throw new SpcValidationException(field, "La limite est hors de la plage admise.");
        }
        return value;
    }

    private static String requireText(String field, String value, int max, String missing) {
        String v = value == null ? "" : value.strip();
        if (v.isEmpty()) {
            throw new SpcValidationException(field, missing);
        }
        if (v.length() > max) {
            throw new SpcValidationException(field, "Ce champ dépasse " + max + " caractères.");
        }
        return v;
    }

    private static String optionalText(String field, String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String v = value.strip();
        if (v.length() > max) {
            throw new SpcValidationException(field, "Ce champ dépasse " + max + " caractères.");
        }
        return v;
    }

    /** Trois décimales : la précision du tableau des formules. */
    private static double arrondi(double v) {
        return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    private static List<List<Double>> copie(List<List<Double>> grille) {
        List<List<Double>> lignes = new ArrayList<>(grille.size());
        for (List<Double> ligne : grille) {
            lignes.add(Collections.unmodifiableList(new ArrayList<>(ligne)));
        }
        return Collections.unmodifiableList(lignes);
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getPartReference() { return partReference; }
    public String getCharacteristic() { return characteristic; }
    public String getSpecification() { return specification; }
    public String getMachine() { return machine; }
    public UUID getLeaderUserId() { return leaderUserId; }
    public String getLeaderName() { return leaderName; }
    public LocalDate getStudyDate() { return studyDate; }
    public boolean isActionPlan() { return actionPlan; }
    public double getLsl() { return lsl; }
    public double getUsl() { return usl; }
    public int getSubgroupSize() { return subgroupSize; }
    public List<List<Double>> getMeasurements() { return measurements; }
    public double getCp() { return cp; }
    public double getCpk() { return cpk; }
    public CapabilityThresholds getThresholds() { return thresholds; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
