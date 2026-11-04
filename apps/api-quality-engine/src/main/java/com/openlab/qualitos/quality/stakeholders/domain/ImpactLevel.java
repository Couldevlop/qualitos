package com.openlab.qualitos.quality.stakeholders.domain;

/**
 * Un niveau à trois crans : faible, moyen, élevé.
 *
 * <p>Sert l'impact qualité, l'impact réglementaire, le niveau de risque de la
 * partie intéressée et celui de chacune de ses exigences. Trois crans et pas
 * cinq : au-delà, la cotation devient une discussion sur l'échelle plutôt que
 * sur le risque.
 */
public enum ImpactLevel {

    LOW(1),
    MEDIUM(2),
    HIGH(3);

    private final int weight;

    ImpactLevel(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }

    /**
     * La criticité globale : le PLUS ÉLEVÉ des deux impacts, jamais leur
     * moyenne.
     *
     * <p>Une partie intéressée dont l'impact réglementaire est élevé est
     * critique même si son impact qualité est faible — une autorité de tutelle
     * ne se dilue pas dans une moyenne. La moyenne ferait passer « faible et
     * élevé » pour « moyen », c'est-à-dire ferait disparaître le seul des deux
     * qui compte.
     */
    public static ImpactLevel worst(ImpactLevel a, ImpactLevel b) {
        if (a == null && b == null) {
            return LOW;
        }
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return a.weight >= b.weight ? a : b;
    }
}
