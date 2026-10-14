package com.openlab.qualitos.quality.change.domain;

/**
 * Le niveau d'impact d'un domaine, et les points qu'il apporte au score.
 *
 * <p>Aucun 0, Faible 1, Moyen 2, Élevé 3 : le score d'une demande rapporte la
 * somme pondérée de ces points au maximum possible (tout « Élevé »).
 */
public enum ImpactLevel {
    NONE(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    /** Le niveau le plus haut : c'est lui qui fixe le dénominateur du score. */
    public static final int MAX_POINTS = 3;

    private final int points;

    ImpactLevel(int points) {
        this.points = points;
    }

    public int points() {
        return points;
    }
}
