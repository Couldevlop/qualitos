package com.openlab.qualitos.quality.dmaic;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Calcule les indices de capabilite Cp/Cpk + statistiques descriptives.
 *
 * Formules :
 *   Cp  = (USL - LSL) / (6 * sigma)
 *   Cpu = (USL - mean) / (3 * sigma)
 *   Cpl = (mean - LSL) / (3 * sigma)
 *   Cpk = min(Cpu, Cpl)
 *
 * Si une seule borne (USL ou LSL) est fournie, Cp n'est pas calculable mais
 * Cpk = Cpu ou Cpl selon le cas.
 *
 * Le niveau Sigma est approxime par Cpk * 3 (regle empirique Six Sigma).
 *
 * Sample stdDev utilise n-1 (estimateur non biaise).
 */
@Component
public class CapabilityCalculator {

    /** Tailles d'echantillon recommandees minimum pour Cp/Cpk fiables. */
    public static final int MIN_RECOMMENDED_SAMPLES = 30;

    public DmaicDto.CapabilityResponse compute(List<Double> values,
                                               Double specLowerLimit,
                                               Double specUpperLimit,
                                               Double specTarget) {
        List<String> warnings = new ArrayList<>();

        if (values == null || values.isEmpty()) {
            return empty(specLowerLimit, specUpperLimit, specTarget,
                    List.of("Pas de donnees — au moins 1 mesure requise"));
        }

        int n = values.size();
        if (n < MIN_RECOMMENDED_SAMPLES) {
            warnings.add("Echantillon faible (n=" + n + ", recommande >= " + MIN_RECOMMENDED_SAMPLES + ")");
        }

        double mean = mean(values);
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElseThrow();
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElseThrow();
        double stdDev = stdDev(values, mean);

        Double cp = null, cpk = null, cpu = null, cpl = null, sigma = null;
        if (stdDev > 0d) {
            if (specUpperLimit != null) {
                cpu = (specUpperLimit - mean) / (3d * stdDev);
            }
            if (specLowerLimit != null) {
                cpl = (mean - specLowerLimit) / (3d * stdDev);
            }
            if (specLowerLimit != null && specUpperLimit != null) {
                cp = (specUpperLimit - specLowerLimit) / (6d * stdDev);
            }
            if (cpu != null && cpl != null) {
                cpk = Math.min(cpu, cpl);
            } else if (cpu != null) {
                cpk = cpu;
            } else if (cpl != null) {
                cpk = cpl;
            }
            if (cpk != null) {
                sigma = cpk * 3d;
            }
        } else {
            warnings.add("Ecart-type nul (toutes les mesures identiques) — Cp/Cpk non calculables");
        }

        String interpretation = interpret(cpk);

        return new DmaicDto.CapabilityResponse(
                n, mean, stdDev, min, max,
                specLowerLimit, specUpperLimit, specTarget,
                cp, cpk, cpu, cpl, sigma,
                interpretation, warnings);
    }

    static double mean(List<Double> values) {
        double sum = 0d;
        for (Double v : values) sum += v;
        return sum / values.size();
    }

    static double stdDev(List<Double> values, double mean) {
        int n = values.size();
        if (n < 2) return 0d;
        double sumSq = 0d;
        for (Double v : values) {
            double d = v - mean;
            sumSq += d * d;
        }
        return Math.sqrt(sumSq / (n - 1));
    }

    /**
     * Interpretation pragmatique du Cpk (referencee dans la litterature qualite).
     * Bornes : 1.33 = capable, 1.67 = tres capable, 2.0 = 6-sigma classique.
     */
    static String interpret(Double cpk) {
        if (cpk == null) return "Indeterminé";
        if (cpk < 0)    return "Processus hors specs — produit majoritairement non conforme";
        if (cpk < 1)    return "Processus non capable (< 1)";
        if (cpk < 1.33) return "Processus marginalement capable (1 ≤ Cpk < 1.33)";
        if (cpk < 1.67) return "Processus capable (1.33 ≤ Cpk < 1.67)";
        if (cpk < 2.0)  return "Processus très capable (1.67 ≤ Cpk < 2.0)";
        return "Processus Six Sigma (Cpk ≥ 2.0)";
    }

    private DmaicDto.CapabilityResponse empty(Double lsl, Double usl, Double target, List<String> warnings) {
        return new DmaicDto.CapabilityResponse(
                0, null, null, null, null,
                lsl, usl, target,
                null, null, null, null, null,
                "Indeterminé", warnings);
    }
}
