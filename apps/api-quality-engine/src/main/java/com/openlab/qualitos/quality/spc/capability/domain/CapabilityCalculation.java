package com.openlab.qualitos.quality.spc.capability.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Le calcul d'une étude de capabilité, formule par formule.
 *
 * <p>Les formules sont celles du classeur de référence
 * ({@code docs/spc/SPC Cp Cpk Study Template.xlsx}), écrites une fois pour les
 * deux cartes ({@link ChartFactors}) :
 * <ul>
 *   <li>X̄ et la dispersion (R ou s) par sous-groupe, puis X̿ = Σ X̄ / m et la
 *       dispersion moyenne = Σ dispersion / m ;</li>
 *   <li>σ = dispersion moyenne / d2 (carte R) ou / c4 (carte S) ;
 *       Cp = (USL − LSL) / 6σ ; CPU = (USL − X̿) / 3σ ;
 *       CPL = (X̿ − LSL) / 3σ ; Cpk = min(CPU, CPL) ;</li>
 *   <li>carte X̄ : X̿ ± A2 × R̄ (ou ± A3 × s̄) ; carte de dispersion : entre
 *       D3 × R̄ et D4 × R̄ (ou B3 × s̄ et B4 × s̄).</li>
 * </ul>
 *
 * <p><b>Les composantes nommées « range » portent la dispersion de la carte
 * retenue</b> : l'étendue sur une carte R, l'écart-type d'échantillon sur une
 * carte S. Le nom est celui du contrat d'origine, gardé pour ne pas casser les
 * lecteurs ; {@link #factors()} dit de quelle carte il s'agit.
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
        ChartFactors factors,
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

    /**
     * Les bornes du nombre de sous-groupes d'une étude.
     *
     * <p>Le classeur de référence en comptait vingt, et vingt avait été figé
     * des deux côtés : une étude de douze sous-groupes — le cas courant d'une
     * collecte en cours — était refusée. La règle de l'art (« vingt à
     * vingt-cinq sous-groupes ») est un plancher de CONFIANCE, que l'écran
     * rappelle, pas une taille de grille.
     *
     * <p>Le plafond n'est pas statistique : aucune formule ne se dégrade quand
     * les relevés s'accumulent, au contraire. Il ne protège que la taille du
     * corps de requête et la largeur de la grille affichable. Cent sous-groupes
     * de vingt-cinq mesures, c'est 2 500 nombres : une campagne longue y tient.
     */
    public static final int MIN_SUBGROUPS = 2;
    public static final int DEFAULT_SUBGROUPS = 20;
    public static final int MAX_SUBGROUPS = 100;

    public CapabilityCalculation {
        subgroupMeans = Collections.unmodifiableList(new ArrayList<>(subgroupMeans));
        subgroupRanges = Collections.unmodifiableList(new ArrayList<>(subgroupRanges));
    }

    /**
     * Calcule l'étude.
     *
     * @param measurements {@code n} lignes (échantillons) d'autant de cellules
     *                     que l'étude compte de sous-groupes, une cellule vide
     *                     valant {@code null}
     */
    public static CapabilityCalculation of(double lsl, double usl, int n, List<List<Double>> measurements) {
        ChartFactors k = ChartFactors.forSubgroupSize(n);
        // La largeur est celle de la ligne la plus longue : une grille en escalier
        // ne perd pas ses dernieres colonnes, ses sous-groupes incomplets sont
        // simplement ecartes plus bas, comme une cellule vide.
        int colonnes = measurements == null ? 0 : measurements.stream()
                .filter(java.util.Objects::nonNull).mapToInt(List::size).max().orElse(0);
        List<Double> moyennes = new ArrayList<>(colonnes);
        List<Double> etendues = new ArrayList<>(colonnes);
        double sommeMoyennes = 0;
        double sommeEtendues = 0;
        int m = 0;
        for (int c = 0; c < colonnes; c++) {
            double[] relevees = new double[n];
            double somme = 0;
            int presentes = 0;
            for (int r = 0; r < n; r++) {
                Double v = cell(measurements, r, c);
                if (v != null) {
                    relevees[presentes] = v;
                    somme += v;
                    presentes++;
                }
            }
            if (presentes == n) {
                double moyenne = somme / n;
                double dispersion = k.dispersionOf(relevees);
                moyennes.add(moyenne);
                etendues.add(dispersion);
                sommeMoyennes += moyenne;
                sommeEtendues += dispersion;
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
        double sigma = rb / k.sigmaDivisor();
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
                xbb + k.meanFactor() * rb, xbb - k.meanFactor() * rb,
                k.dispersionUpper() * rb, k.dispersionLower() * rb,
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
