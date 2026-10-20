package com.openlab.qualitos.quality.spc.capability.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Le calcul d'une étude de capabilité X̄ – R, formule par formule.
 *
 * <p>Les formules sont celles du classeur de référence
 * ({@code docs/spc/SPC Cp Cpk Study Template.xlsx}) :
 * <ul>
 *   <li>X̄ et R par sous-groupe, puis X̿ = Σ X̄ / m et R̄ = Σ R / m ;</li>
 *   <li>σ = R̄ / d2 ; Cp = (USL − LSL) / 6σ ; CPU = (USL − X̿) / 3σ ;
 *       CPL = (X̿ − LSL) / 3σ ; Cpk = min(CPU, CPL) ;</li>
 *   <li>carte X̄ : X̿ ± A2 × R̄ ; carte R : D4 × R̄ et D3 × R̄.</li>
 * </ul>
 *
 * <p><b>Seuls les sous-groupes complets comptent.</b> Un sous-groupe à qui il
 * manque une mesure n'a ni moyenne ni étendue comparables aux autres ; le
 * classeur divisait par 20 en supposant la grille pleine, ici on divise par le
 * nombre de sous-groupes complets {@code m} — c'est la même chose quand la
 * grille est pleine, et c'est juste quand elle ne l'est pas.
 *
 * <p>Les indices de capabilité sont nuls tant qu'il n'y a aucun sous-groupe
 * complet ou aucune dispersion (σ = 0) : afficher une valeur dirait le contraire
 * de la réalité.
 */
public record CapabilityCalculation(
        int subgroupSize,
        SpcConstants constants,
        List<Double> subgroupMeans,
        List<Double> subgroupRanges,
        int completeSubgroups,
        double lsl,
        double usl,
        Double sumOfMeans,
        Double grandMean,
        Double meanRange,
        Double sigma,
        Double cp,
        Double cpu,
        Double cpl,
        Double cpk,
        Double meanUcl,
        Double meanLcl,
        Double rangeUcl,
        Double rangeLcl,
        Double dispersionPercent,
        Double centerOffset,
        Double centeringPercent) {

    /** Le nombre de sous-groupes d'une étude, colonnes « SG 01 » à « SG 20 » du classeur. */
    public static final int SUBGROUPS = 20;

    public CapabilityCalculation {
        subgroupMeans = Collections.unmodifiableList(new ArrayList<>(subgroupMeans));
        subgroupRanges = Collections.unmodifiableList(new ArrayList<>(subgroupRanges));
    }

    /**
     * Calcule l'étude.
     *
     * @param measurements {@code n} lignes (échantillons) de {@link #SUBGROUPS}
     *                     cellules (sous-groupes), une cellule vide valant {@code null}
     */
    public static CapabilityCalculation of(double lsl, double usl, int n, List<List<Double>> measurements) {
        SpcConstants k = SpcConstants.forSubgroupSize(n);
        List<Double> moyennes = new ArrayList<>(SUBGROUPS);
        List<Double> etendues = new ArrayList<>(SUBGROUPS);
        double sommeMoyennes = 0;
        double sommeEtendues = 0;
        int m = 0;
        for (int c = 0; c < SUBGROUPS; c++) {
            double somme = 0;
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            int presentes = 0;
            for (int r = 0; r < n; r++) {
                Double v = cell(measurements, r, c);
                if (v != null) {
                    somme += v;
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                    presentes++;
                }
            }
            if (presentes == n) {
                double moyenne = somme / n;
                double etendue = max - min;
                moyennes.add(moyenne);
                etendues.add(etendue);
                sommeMoyennes += moyenne;
                sommeEtendues += etendue;
                m++;
            } else {
                moyennes.add(null);
                etendues.add(null);
            }
        }
        if (m == 0) {
            return new CapabilityCalculation(n, k, moyennes, etendues, 0, lsl, usl,
                    null, null, null, null, null, null, null, null, null, null, null, null,
                    null, null, null);
        }
        double xbb = sommeMoyennes / m;
        double rb = sommeEtendues / m;
        double sigma = rb / k.d2();
        double tolerance = usl - lsl;
        double milieu = (usl + lsl) / 2;
        double decalage = xbb - milieu;
        boolean disperse = sigma > 0;
        boolean limitesValides = tolerance > 0;
        Double cpu = disperse ? (usl - xbb) / (3 * sigma) : null;
        Double cpl = disperse ? (xbb - lsl) / (3 * sigma) : null;
        return new CapabilityCalculation(n, k, moyennes, etendues, m, lsl, usl,
                sommeMoyennes, xbb, rb, sigma,
                disperse ? tolerance / (6 * sigma) : null,
                cpu, cpl,
                disperse ? Math.min(cpu, cpl) : null,
                xbb + k.a2() * rb, xbb - k.a2() * rb,
                k.d4() * rb, k.d3() * rb,
                limitesValides ? 6 * sigma / tolerance * 100 : null,
                decalage,
                limitesValides ? Math.abs(decalage) / (tolerance / 2) * 100 : null);
    }

    /** Le calcul aboutit à un Cp et un Cpk : au moins un sous-groupe complet, et de la dispersion. */
    public boolean computable() {
        return cp != null && cpk != null;
    }

    private static Double cell(List<List<Double>> grille, int r, int c) {
        if (grille == null || r >= grille.size()) {
            return null;
        }
        List<Double> ligne = grille.get(r);
        return ligne == null || c >= ligne.size() ? null : ligne.get(c);
    }
}
