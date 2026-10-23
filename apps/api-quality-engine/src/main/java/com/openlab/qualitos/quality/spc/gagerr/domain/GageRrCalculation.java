package com.openlab.qualitos.quality.spc.gagerr.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Le calcul complet d'une étude R&amp;R, fait d'une traite sur une grille
 * complète : la méthode « moyenne et étendue » des onglets « Données » et
 * « Résultats » du classeur {@code docs/spc/Etude_RR_Gage_1.xlsx}, puis
 * l'ANOVA à deux facteurs avec interaction (méthode de référence AIAG).
 *
 * <p>La grille se lit {@code grid[opérateur][pièce][essai]}. Toutes les
 * cellules sont renseignées : la méthode n'a pas de sens sur une grille trouée,
 * et le domaine la refuse avant d'arriver ici.
 *
 * @param cellMeans       moyenne des essais, par opérateur puis par pièce
 * @param cellRanges      étendue des essais (max − min), par opérateur puis par pièce
 * @param trialMeans      moyenne de chaque colonne d'essai (ligne « Moyenne » du classeur)
 * @param operatorMeans   X̄ de chaque opérateur
 * @param operatorRanges  R̄ de chaque opérateur
 * @param partMeans       moyenne de chaque pièce, toutes mesures confondues
 * @param rangesBeyondLimit nombre d'étendues au-dessus de UCL<sub>R</sub>
 * @param ndc             catégories distinctes : ⌊1,41 × PV / GRR⌋, 0 si GRR est nul
 * @param repeatabilityDominant EV &gt; AV : le poste dominant est l'instrument, sinon les opérateurs
 */
public record GageRrCalculation(
        int operators, int parts, int trials, double multiplier, double tolerance,
        List<List<Double>> cellMeans,
        List<List<Double>> cellRanges,
        List<List<Double>> trialMeans,
        List<Double> operatorMeans,
        List<Double> operatorRanges,
        List<Double> partMeans,
        double grandMean,
        double xDiff,
        double meanRange,
        double partRange,
        double rangeUcl,
        double maxRange,
        int rangesBeyondLimit,
        GageRrConstants constants,
        VariationComponent ev,
        VariationComponent av,
        VariationComponent grr,
        VariationComponent pv,
        VariationComponent tv,
        int ndc,
        GageRrDecision decision,
        boolean repeatabilityDominant,
        GageRrAnova anova) {

    /** Le facteur du nombre de catégories distinctes (√2, arrondi AIAG). */
    public static final double NDC_FACTOR = 1.41;
    /** Le seuil AIAG d'une discrimination suffisante. */
    public static final int NDC_SUFFICIENT = 5;
    /** En deçà de cette part de la somme totale des carrés, un reste est un arrondi. */
    static final double RESIDUAL = 1e-12;

    /** Il y a de la variation à décomposer : sans elle, aucun pourcentage n'a de sens. */
    public boolean computable() {
        return tv.value() > 0;
    }

    public boolean ndcSufficient() {
        return ndc >= NDC_SUFFICIENT;
    }

    public static GageRrCalculation of(double lsl, double usl, double multiplier, List<List<List<Double>>> grid) {
        int k = grid.size();
        int n = grid.get(0).size();
        int r = grid.get(0).get(0).size();
        GageRrConstants c = GageRrConstants.forPlan(k, n, r, multiplier);
        double tolerance = usl - lsl;

        // ---------- étape 1 : moyennes et étendues ----------
        List<List<Double>> cellMeans = new ArrayList<>();
        List<List<Double>> cellRanges = new ArrayList<>();
        List<List<Double>> trialMeans = new ArrayList<>();
        List<Double> operatorMeans = new ArrayList<>();
        List<Double> operatorRanges = new ArrayList<>();
        double[] sommePieces = new double[n];
        double somme = 0;
        double maxRange = 0;
        for (List<List<Double>> operateur : grid) {
            List<Double> moyennes = new ArrayList<>();
            List<Double> etendues = new ArrayList<>();
            double[] sommeEssais = new double[r];
            for (int p = 0; p < n; p++) {
                List<Double> essais = operateur.get(p);
                double s = 0;
                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                for (int t = 0; t < r; t++) {
                    double v = essais.get(t);
                    s += v;
                    sommeEssais[t] += v;
                    min = Math.min(min, v);
                    max = Math.max(max, v);
                }
                sommePieces[p] += s;
                somme += s;
                moyennes.add(s / r);
                etendues.add(max - min);
                maxRange = Math.max(maxRange, max - min);
            }
            List<Double> colonnes = new ArrayList<>();
            for (double se : sommeEssais) {
                colonnes.add(se / n);
            }
            cellMeans.add(List.copyOf(moyennes));
            cellRanges.add(List.copyOf(etendues));
            trialMeans.add(List.copyOf(colonnes));
            operatorMeans.add(moyenne(moyennes));
            operatorRanges.add(moyenne(etendues));
        }
        List<Double> partMeans = new ArrayList<>();
        for (double sp : sommePieces) {
            partMeans.add(sp / (k * r));
        }
        double grandMean = somme / (k * n * r);
        double xDiff = etendue(operatorMeans);
        double meanRange = moyenne(operatorRanges);
        double partRange = etendue(partMeans);
        double rangeUcl = c.d4() * meanRange;
        int beyond = 0;
        for (List<Double> etendues : cellRanges) {
            for (double e : etendues) {
                if (e > rangeUcl) {
                    beyond++;
                }
            }
        }

        // ---------- étape 3 : décomposition ----------
        double ev = meanRange * c.k1();
        double av = Math.sqrt(Math.max(0, Math.pow(xDiff * c.k2(), 2) - ev * ev / (n * r)));
        double grr = Math.hypot(ev, av);
        double pv = partRange * c.k3();
        double tv = Math.hypot(grr, pv);

        // ---------- étape 4 : aptitude ----------
        GageRrAnova anova = anova(grid, cellMeans, partMeans, operatorMeans, grandMean, multiplier, tolerance);
        return new GageRrCalculation(k, n, r, multiplier, tolerance,
                List.copyOf(cellMeans), List.copyOf(cellRanges), List.copyOf(trialMeans),
                List.copyOf(operatorMeans), List.copyOf(operatorRanges), List.copyOf(partMeans),
                grandMean, xDiff, meanRange, partRange, rangeUcl, maxRange, beyond, c,
                VariationComponent.of(ev, tv, tolerance), VariationComponent.of(av, tv, tolerance),
                VariationComponent.of(grr, tv, tolerance), VariationComponent.of(pv, tv, tolerance),
                VariationComponent.of(tv, tv, tolerance),
                ndc(pv, grr), GageRrDecision.of(tv > 0 ? grr / tv : 0), ev > av, anova);
    }

    /** INT(1,41 × PV / GRR), et 0 quand le système de mesure ne varie pas (cellule D37). */
    static int ndc(double pv, double grr) {
        return grr > 0 ? (int) Math.floor(NDC_FACTOR * pv / grr) : 0;
    }

    // ---------- ANOVA ----------

    @SuppressWarnings("java:S107") // les sommes déjà faites par l'étape 1, passées plutôt que recalculées
    private static GageRrAnova anova(List<List<List<Double>>> grid, List<List<Double>> cellMeans,
                                     List<Double> partMeans, List<Double> operatorMeans, double grandMean,
                                     double multiplier, double tolerance) {
        int k = grid.size();
        int n = partMeans.size();
        int r = grid.get(0).get(0).size();

        double ssParts = 0;
        for (double pm : partMeans) {
            ssParts += (pm - grandMean) * (pm - grandMean);
        }
        ssParts *= k * r;
        double ssOperators = 0;
        for (double om : operatorMeans) {
            ssOperators += (om - grandMean) * (om - grandMean);
        }
        ssOperators *= n * r;
        double ssTotal = 0;
        double ssRepeat = 0;
        for (int o = 0; o < k; o++) {
            for (int p = 0; p < n; p++) {
                double cm = cellMeans.get(o).get(p);
                for (double v : grid.get(o).get(p)) {
                    ssTotal += (v - grandMean) * (v - grandMean);
                    ssRepeat += (v - cm) * (v - cm);
                }
            }
        }
        // L'interaction est le reste de la somme totale : un résidu d'arrondi (négatif, ou de
        // l'ordre de 1e-16 quand les effets sont purement additifs) n'est pas une interaction.
        double reste = ssTotal - ssParts - ssOperators - ssRepeat;
        double ssInteraction = reste <= RESIDUAL * ssTotal ? 0 : reste;

        int dfParts = n - 1;
        int dfOperators = k - 1;
        int dfInteraction = (n - 1) * (k - 1);
        int dfRepeat = n * k * (r - 1);
        int dfTotal = n * k * r - 1;
        double msParts = ssParts / dfParts;
        double msOperators = ssOperators / dfOperators;
        double msInteraction = ssInteraction / dfInteraction;
        double msRepeat = ssRepeat / dfRepeat;

        Double fInteraction = ratio(msInteraction, msRepeat);
        Double pInteraction = pValue(fInteraction, dfInteraction, dfRepeat);
        List<GageRrAnova.AnovaRow> full = List.of(
                row(GageRrAnova.Source.PART, dfParts, ssParts, msParts, msInteraction, dfInteraction),
                row(GageRrAnova.Source.OPERATOR, dfOperators, ssOperators, msOperators, msInteraction, dfInteraction),
                new GageRrAnova.AnovaRow(GageRrAnova.Source.INTERACTION, dfInteraction, ssInteraction,
                        msInteraction, fInteraction, pInteraction),
                new GageRrAnova.AnovaRow(GageRrAnova.Source.REPEATABILITY, dfRepeat, ssRepeat, msRepeat, null, null),
                new GageRrAnova.AnovaRow(GageRrAnova.Source.TOTAL, dfTotal, ssTotal, null, null, null));

        // Sans dispersion de répétabilité, F n'existe pas : on regroupe seulement s'il n'y a pas d'interaction.
        boolean pooled = pInteraction == null ? msInteraction == 0 : pInteraction > GageRrAnova.POOLING_P;
        List<GageRrAnova.AnovaRow> pooledTable = null;
        double repeatability;
        double interaction;
        double operator;
        double part;
        if (pooled) {
            int dfPool = dfInteraction + dfRepeat;
            double msPool = (ssInteraction + ssRepeat) / dfPool;
            pooledTable = List.of(
                    row(GageRrAnova.Source.PART, dfParts, ssParts, msParts, msPool, dfPool),
                    row(GageRrAnova.Source.OPERATOR, dfOperators, ssOperators, msOperators, msPool, dfPool),
                    new GageRrAnova.AnovaRow(GageRrAnova.Source.REPEATABILITY, dfPool, ssInteraction + ssRepeat,
                            msPool, null, null),
                    new GageRrAnova.AnovaRow(GageRrAnova.Source.TOTAL, dfTotal, ssTotal, null, null, null));
            repeatability = msPool;
            interaction = 0;
            operator = Math.max(0, (msOperators - msPool) / (n * r));
            part = Math.max(0, (msParts - msPool) / (k * r));
        } else {
            repeatability = msRepeat;
            interaction = Math.max(0, (msInteraction - msRepeat) / r);
            operator = Math.max(0, (msOperators - msInteraction) / (n * r));
            part = Math.max(0, (msParts - msInteraction) / (k * r));
        }
        double reproducibility = operator + interaction;
        double grrVar = repeatability + reproducibility;
        double totalVar = grrVar + part;
        GageRrAnova.Variance variance = new GageRrAnova.Variance(repeatability, operator, interaction,
                reproducibility, grrVar, part, totalVar);

        double tv = multiplier * Math.sqrt(totalVar);
        double grr = multiplier * Math.sqrt(grrVar);
        double pv = multiplier * Math.sqrt(part);
        return new GageRrAnova(full, pooledTable, pooled, variance,
                VariationComponent.of(multiplier * Math.sqrt(repeatability), tv, tolerance),
                VariationComponent.of(multiplier * Math.sqrt(reproducibility), tv, tolerance),
                VariationComponent.of(multiplier * Math.sqrt(operator), tv, tolerance),
                VariationComponent.of(multiplier * Math.sqrt(interaction), tv, tolerance),
                VariationComponent.of(grr, tv, tolerance),
                VariationComponent.of(pv, tv, tolerance),
                VariationComponent.of(tv, tv, tolerance),
                ndc(pv, grr), GageRrDecision.of(tv > 0 ? grr / tv : 0));
    }

    private static GageRrAnova.AnovaRow row(GageRrAnova.Source source, int df, double ss, double ms,
                                            double denominator, int dfDenominator) {
        Double f = ratio(ms, denominator);
        return new GageRrAnova.AnovaRow(source, df, ss, ms, f, pValue(f, df, dfDenominator));
    }

    private static Double ratio(double numerator, double denominator) {
        return denominator > 0 ? numerator / denominator : null;
    }

    private static Double pValue(Double f, int d1, int d2) {
        return f == null ? null : FDistribution.survival(f, d1, d2);
    }

    private static double moyenne(List<Double> valeurs) {
        double s = 0;
        for (double v : valeurs) {
            s += v;
        }
        return s / valeurs.size();
    }

    private static double etendue(List<Double> valeurs) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double v : valeurs) {
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        return max - min;
    }
}
