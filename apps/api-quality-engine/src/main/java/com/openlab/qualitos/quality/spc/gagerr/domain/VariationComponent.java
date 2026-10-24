package com.openlab.qualitos.quality.spc.gagerr.domain;

/**
 * Une source de variation et ses trois lectures (colonnes D à G de l'onglet
 * « Résultats »).
 *
 * @param value               la dispersion, dans l'unité de la mesure (multiplicateur σ inclus)
 * @param percentOfTotal      valeur / TV, en fraction — ne s'additionne pas (somme quadratique)
 * @param percentContribution (valeur / TV)², en fraction — EV + AV + PV = 100 %
 * @param percentTolerance    valeur / (USL − LSL), en fraction
 */
public record VariationComponent(double value, double percentOfTotal, double percentContribution,
                                 double percentTolerance) {

    static VariationComponent of(double value, double total, double tolerance) {
        double ratio = total > 0 ? value / total : 0;
        return new VariationComponent(value, ratio, ratio * ratio, tolerance > 0 ? value / tolerance : 0);
    }
}
