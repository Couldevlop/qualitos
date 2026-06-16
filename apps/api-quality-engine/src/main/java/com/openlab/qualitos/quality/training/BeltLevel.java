package com.openlab.qualitos.quality.training;

/**
 * Niveau de ceinture qualité (gamification — CLAUDE.md §19.3 « Yellow Belt → Black Belt »).
 *
 * <p>La progression suit le vocabulaire Lean Six Sigma : on démarre WHITE
 * (apprenant inscrit, aucun acquis), puis YELLOW, GREEN, BLACK selon le total
 * de points accumulés. Les seuils sont des invariants métier purs, testés
 * unitairement dans {@link #fromPoints(int)}.</p>
 *
 * <p>L'ordre de déclaration EST l'ordre de progression — ne pas réordonner sans
 * adapter {@link #fromPoints(int)}.</p>
 */
public enum BeltLevel {

    WHITE(0),
    YELLOW(100),
    GREEN(300),
    BLACK(700);

    private final int minPoints;

    BeltLevel(int minPoints) {
        this.minPoints = minPoints;
    }

    /** Seuil de points (inclusif) à partir duquel cette ceinture est atteinte. */
    public int minPoints() {
        return minPoints;
    }

    /**
     * Calcule la ceinture correspondant à un total de points (fonction pure).
     *
     * @param points total de points de l'apprenant (négatif traité comme 0)
     * @return la ceinture la plus élevée dont le seuil est atteint
     */
    public static BeltLevel fromPoints(int points) {
        BeltLevel result = WHITE;
        for (BeltLevel level : values()) {
            if (points >= level.minPoints) {
                result = level;
            }
        }
        return result;
    }

    /**
     * Points restant à gagner pour atteindre la ceinture suivante.
     *
     * @param points total courant de l'apprenant
     * @return points manquants vers la prochaine ceinture, ou 0 si BLACK (palier max)
     */
    public static int pointsToNext(int points) {
        BeltLevel current = fromPoints(points);
        BeltLevel[] all = values();
        if (current.ordinal() == all.length - 1) {
            return 0;
        }
        BeltLevel next = all[current.ordinal() + 1];
        return Math.max(0, next.minPoints - Math.max(0, points));
    }
}
