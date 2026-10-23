package com.openlab.qualitos.quality.spc.gagerr.domain;

import com.openlab.qualitos.quality.spc.capability.domain.SpcValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Une étude de répétabilité et de reproductibilité (Gage R&amp;R) d'un moyen
 * de mesure.
 *
 * <p><b>Le serveur calcule, à chaque écriture.</b> L'écran refait le calcul
 * pour suivre la frappe, mais le %GRR, le ndc et le verdict enregistrés — ceux
 * que lit le registre — viennent d'ici : la libération d'un instrument ne doit
 * pas dépendre du navigateur qui l'a saisie.
 *
 * <p><b>Le verdict est celui du classeur</b> : méthode « moyenne et étendue »,
 * %GRR de la variation totale (cellule C38). L'ANOVA, plus précise, est
 * enregistrée à côté ({@code anovaGrrPercent}, {@code anovaNdc}) et affichée
 * en regard ; elle ne change pas le verdict.
 */
public final class GageRrStudy {

    public static final int PART_REFERENCE_MAX = 120;
    public static final int CHARACTERISTIC_MAX = 150;
    public static final int INSTRUMENT_MAX = 150;
    public static final int PERSON_NAME_MAX = 150;
    /** Au-delà, une mesure est une faute de frappe, pas une grandeur physique. */
    static final double MEASUREMENT_MAX = 1e12;

    private final UUID id;
    private final UUID tenantId;
    private String partReference;
    private String characteristic;
    private String instrument;
    private double lsl;
    private double usl;
    private int operatorCount;
    private int partCount;
    private int trialCount;
    private double sigmaMultiplier;
    private LocalDate studyDate;
    private UUID responsibleUserId;
    private String responsibleName;
    private List<GageRrOperator> operators;
    private List<List<List<Double>>> measurements;
    private double grrPercent;
    private double grrTolerancePercent;
    private int ndc;
    private GageRrDecision decision;
    private double anovaGrrPercent;
    private int anovaNdc;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;

    @SuppressWarnings("java:S107") // reconstitution depuis la persistance : un champ par colonne
    public GageRrStudy(UUID id, UUID tenantId, String partReference, String characteristic, String instrument,
                       double lsl, double usl, int operatorCount, int partCount, int trialCount,
                       double sigmaMultiplier, LocalDate studyDate, UUID responsibleUserId, String responsibleName,
                       List<GageRrOperator> operators, List<List<List<Double>>> measurements,
                       double grrPercent, double grrTolerancePercent, int ndc, GageRrDecision decision,
                       double anovaGrrPercent, int anovaNdc, UUID createdBy, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.partReference = partReference;
        this.characteristic = characteristic;
        this.instrument = instrument;
        this.lsl = lsl;
        this.usl = usl;
        this.operatorCount = operatorCount;
        this.partCount = partCount;
        this.trialCount = trialCount;
        this.sigmaMultiplier = sigmaMultiplier;
        this.studyDate = studyDate;
        this.responsibleUserId = responsibleUserId;
        this.responsibleName = responsibleName;
        this.operators = List.copyOf(operators);
        this.measurements = copie(measurements);
        this.grrPercent = grrPercent;
        this.grrTolerancePercent = grrTolerancePercent;
        this.ndc = ndc;
        this.decision = decision;
        this.anovaGrrPercent = anovaGrrPercent;
        this.anovaNdc = anovaNdc;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Une étude neuve, validée et calculée. */
    public static GageRrStudy record(UUID tenantId, GageRrStudyDetails details, UUID createdBy, Instant now) {
        Objects.requireNonNull(tenantId, "tenantId");
        GageRrStudy etude = new GageRrStudy(null, tenantId, null, null, null, 0, 0, 0, 0, 0,
                GageRrConstants.REFERENCE_MULTIPLIER, null, null, null, List.of(), List.of(), 0, 0, 0,
                GageRrDecision.NOT_ACCEPTABLE, 0, 0, createdBy, now, now);
        etude.apply(details, now);
        return etude;
    }

    /** Corrige l'étude : tout est revalidé et recalculé d'un bloc. */
    public void revise(GageRrStudyDetails details, Instant now) {
        apply(details, now);
    }

    /** Le calcul complet, refait depuis la grille enregistrée. */
    public GageRrCalculation calculation() {
        return GageRrCalculation.of(lsl, usl, sigmaMultiplier, measurements);
    }

    private void apply(GageRrStudyDetails d, Instant now) {
        if (d == null) {
            throw new SpcValidationException("partReference", "La référence de la pièce est obligatoire.");
        }
        String reference = requireText("partReference", d.partReference(), PART_REFERENCE_MAX,
                "La référence de la pièce est obligatoire.");
        String caracteristique = optionalText("characteristic", d.characteristic(), CHARACTERISTIC_MAX);
        String moyen = optionalText("instrument", d.instrument(), INSTRUMENT_MAX);
        String responsable = optionalText("responsibleName", d.responsibleName(), PERSON_NAME_MAX);
        if (d.responsibleUserId() != null && responsable == null) {
            throw new SpcValidationException("responsibleName", "Le nom du responsable de l'étude est obligatoire.");
        }
        if (d.responsibleUserId() == null && responsable != null) {
            throw new SpcValidationException("responsibleUserId",
                    "Le responsable se choisit dans l'annuaire.");
        }
        if (d.studyDate() == null) {
            throw new SpcValidationException("studyDate", "La date de l'étude est obligatoire.");
        }
        double basse = requireLimit("lsl", d.lsl(), "La limite inférieure (LSL) est obligatoire.");
        double haute = requireLimit("usl", d.usl(), "La limite supérieure (USL) est obligatoire.");
        if (haute <= basse) {
            throw new SpcValidationException("usl", "La limite supérieure doit dépasser la limite inférieure.");
        }
        int k = requireCount("operatorCount", d.operatorCount(), "Le nombre d'opérateurs est obligatoire.");
        int n = requireCount("partCount", d.partCount(), "Le nombre de pièces est obligatoire.");
        int r = requireCount("trialCount", d.trialCount(), "Le nombre d'essais est obligatoire.");
        if (d.sigmaMultiplier() == null) {
            throw new SpcValidationException("sigmaMultiplier", "Le multiplicateur σ est obligatoire.");
        }
        double multiplicateur = GageRrConstants.requireMultiplier(d.sigmaMultiplier());
        GageRrConstants.forPlan(k, n, r, multiplicateur);
        List<GageRrOperator> personnes = requireOperators(d.operators(), k);
        List<List<List<Double>>> grille = requireGrid(d.measurements(), k, n, r);

        GageRrCalculation calcul = GageRrCalculation.of(basse, haute, multiplicateur, grille);
        if (!calcul.computable()) {
            throw new SpcValidationException("measurements",
                    "Les mesures n'ont aucune dispersion : la variation ne se décompose pas.");
        }

        this.partReference = reference;
        this.characteristic = caracteristique;
        this.instrument = moyen;
        this.lsl = basse;
        this.usl = haute;
        this.operatorCount = k;
        this.partCount = n;
        this.trialCount = r;
        this.sigmaMultiplier = multiplicateur;
        this.studyDate = d.studyDate();
        this.responsibleUserId = d.responsibleUserId();
        this.responsibleName = responsable;
        this.operators = personnes;
        this.measurements = grille;
        this.grrPercent = pourcent(calcul.grr().percentOfTotal());
        this.grrTolerancePercent = pourcent(calcul.grr().percentTolerance());
        this.ndc = calcul.ndc();
        this.decision = calcul.decision();
        this.anovaGrrPercent = pourcent(calcul.anova().grr().percentOfTotal());
        this.anovaNdc = calcul.anova().ndc();
        this.updatedAt = now;
    }

    private static int requireCount(String field, Integer value, String missing) {
        if (value == null) {
            throw new SpcValidationException(field, missing);
        }
        return value;
    }

    /** k personnes de l'annuaire, distinctes : un même opérateur compté deux fois fausserait AV. */
    private static List<GageRrOperator> requireOperators(List<GageRrOperator> operateurs, int k) {
        if (operateurs == null || operateurs.size() != k) {
            throw new SpcValidationException("operators", "Choisissez " + k + " opérateurs dans l'annuaire.");
        }
        Set<UUID> vus = new HashSet<>();
        List<GageRrOperator> propres = new ArrayList<>();
        for (GageRrOperator o : operateurs) {
            if (o == null || o.accountId() == null) {
                throw new SpcValidationException("operators", "Chaque opérateur se choisit dans l'annuaire.");
            }
            String nom = requireText("operators", o.name(), PERSON_NAME_MAX, "Le nom de l'opérateur est obligatoire.");
            if (!vus.add(o.accountId())) {
                throw new SpcValidationException("operators", "Un même opérateur ne peut figurer deux fois.");
            }
            propres.add(new GageRrOperator(o.accountId(), nom));
        }
        return List.copyOf(propres);
    }

    private static List<List<List<Double>>> requireGrid(List<List<List<Double>>> grille, int k, int n, int r) {
        if (grille == null || grille.size() != k) {
            throw new SpcValidationException("measurements", "La grille compte un bloc par opérateur (" + k + ").");
        }
        for (List<List<Double>> bloc : grille) {
            if (bloc == null || bloc.size() != n) {
                throw new SpcValidationException("measurements", "Chaque opérateur mesure " + n + " pièces.");
            }
            for (List<Double> essais : bloc) {
                if (essais == null || essais.size() != r) {
                    throw new SpcValidationException("measurements", "Chaque pièce est mesurée " + r + " fois.");
                }
                for (Double v : essais) {
                    if (v == null) {
                        throw new SpcValidationException("measurements", "Toutes les mesures de la grille sont obligatoires.");
                    }
                    if (!Double.isFinite(v) || Math.abs(v) > MEASUREMENT_MAX) {
                        throw new SpcValidationException("measurements", "Une mesure est hors de la plage admise.");
                    }
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

    /** Une fraction en pourcentage, à deux décimales : la précision du registre. */
    private static double pourcent(double fraction) {
        return BigDecimal.valueOf(fraction * 100).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static List<List<List<Double>>> copie(List<List<List<Double>>> grille) {
        List<List<List<Double>>> blocs = new ArrayList<>(grille.size());
        for (List<List<Double>> bloc : grille) {
            List<List<Double>> lignes = new ArrayList<>(bloc.size());
            for (List<Double> ligne : bloc) {
                lignes.add(Collections.unmodifiableList(new ArrayList<>(ligne)));
            }
            blocs.add(Collections.unmodifiableList(lignes));
        }
        return Collections.unmodifiableList(blocs);
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getPartReference() { return partReference; }
    public String getCharacteristic() { return characteristic; }
    public String getInstrument() { return instrument; }
    public double getLsl() { return lsl; }
    public double getUsl() { return usl; }
    public int getOperatorCount() { return operatorCount; }
    public int getPartCount() { return partCount; }
    public int getTrialCount() { return trialCount; }
    public double getSigmaMultiplier() { return sigmaMultiplier; }
    public LocalDate getStudyDate() { return studyDate; }
    public UUID getResponsibleUserId() { return responsibleUserId; }
    public String getResponsibleName() { return responsibleName; }
    public List<GageRrOperator> getOperators() { return operators; }
    public List<List<List<Double>>> getMeasurements() { return measurements; }
    public double getGrrPercent() { return grrPercent; }
    public double getGrrTolerancePercent() { return grrTolerancePercent; }
    public int getNdc() { return ndc; }
    public GageRrDecision getDecision() { return decision; }
    public double getAnovaGrrPercent() { return anovaGrrPercent; }
    public int getAnovaNdc() { return anovaNdc; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
