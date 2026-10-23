package com.openlab.qualitos.quality.spc.gagerr.domain;

import java.util.List;

/**
 * L'analyse de variance à deux facteurs croisés (pièce × opérateur) avec
 * interaction, méthode de référence de l'AIAG (MSA 4e éd., annexe A).
 *
 * <p>Le tableau complet porte toujours la ligne d'interaction. Quand elle
 * n'est pas significative (p &gt; 0,25, seuil AIAG), elle est regroupée avec
 * la répétabilité : {@code pooledTable} donne alors le tableau réduit, et les
 * composantes de variance sont estimées sur le carré moyen regroupé.
 *
 * @param fullTable       PART, OPERATOR, INTERACTION, REPEATABILITY, TOTAL ;
 *                        F des pièces et des opérateurs rapportés à l'interaction
 * @param pooledTable     PART, OPERATOR, REPEATABILITY (regroupée), TOTAL ; {@code null} sans regroupement
 * @param interactionPooled l'interaction est regroupée avec la répétabilité
 * @param variance        les composantes de variance σ², une par source
 */
public record GageRrAnova(
        List<AnovaRow> fullTable,
        List<AnovaRow> pooledTable,
        boolean interactionPooled,
        Variance variance,
        VariationComponent ev,
        VariationComponent av,
        VariationComponent operator,
        VariationComponent interaction,
        VariationComponent grr,
        VariationComponent pv,
        VariationComponent tv,
        int ndc,
        GageRrDecision decision) {

    /** Le seuil AIAG au-delà duquel l'interaction est regroupée avec la répétabilité. */
    public static final double POOLING_P = 0.25;

    public enum Source { PART, OPERATOR, INTERACTION, REPEATABILITY, TOTAL }

    /**
     * Une ligne du tableau d'ANOVA. Carré moyen, F et p sont nuls quand ils
     * n'ont pas de sens (ligne totale) ou ne se calculent pas (dénominateur nul).
     */
    public record AnovaRow(Source source, int degreesOfFreedom, double sumOfSquares,
                           Double meanSquare, Double f, Double p) {}

    /** σ² de chaque source ; reproductibilité = opérateur + interaction, GRR = répétabilité + reproductibilité. */
    public record Variance(double repeatability, double operator, double interaction, double reproducibility,
                           double grr, double part, double total) {}

    public AnovaRow row(Source source) {
        return fullTable.stream().filter(r -> r.source() == source).findFirst().orElseThrow();
    }
}
