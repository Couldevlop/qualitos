package com.openlab.qualitos.quality.training;

import java.util.EnumSet;
import java.util.Set;

/**
 * Badges de gamification (CLAUDE.md §19.3) attribués selon les jalons de
 * progression d'un apprenant.
 *
 * <p>Les règles d'attribution sont <b>pures</b> et déterministes : à un état
 * donné (nombre de complétions, meilleur score, ceinture) correspond toujours
 * le même ensemble de badges. Cela rend le calcul testable et idempotent — un
 * recalcul ne « perd » jamais un badge déjà mérité.</p>
 */
public enum Badge {

    /** Première complétion réussie d'un parcours/quiz. */
    FIRST_STEPS,
    /** Au moins 5 complétions réussies. */
    DEDICATED_LEARNER,
    /** Au moins 10 complétions réussies. */
    QUALITY_CHAMPION,
    /** Au moins un score parfait (100). */
    PERFECTIONIST,
    /** Ceinture YELLOW atteinte. */
    YELLOW_BELT,
    /** Ceinture GREEN atteinte. */
    GREEN_BELT,
    /** Ceinture BLACK atteinte. */
    BLACK_BELT;

    /**
     * Détermine l'ensemble des badges mérités pour un état donné (fonction pure).
     *
     * @param completedCount nombre de complétions réussies (négatif traité comme 0)
     * @param bestScore       meilleur score obtenu (0-100), null si aucune complétion
     * @param belt            ceinture courante de l'apprenant
     * @return ensemble immuable des badges mérités
     */
    public static Set<Badge> evaluate(int completedCount, Integer bestScore, BeltLevel belt) {
        Set<Badge> earned = EnumSet.noneOf(Badge.class);
        int completions = Math.max(0, completedCount);

        if (completions >= 1) earned.add(FIRST_STEPS);
        if (completions >= 5) earned.add(DEDICATED_LEARNER);
        if (completions >= 10) earned.add(QUALITY_CHAMPION);
        if (bestScore != null && bestScore >= 100) earned.add(PERFECTIONIST);

        // Les badges de ceinture sont cumulatifs : atteindre BLACK octroie aussi
        // YELLOW et GREEN (jalons franchis), conformément au parcours de montée.
        switch (belt) {
            case BLACK:
                earned.add(BLACK_BELT);
                earned.add(GREEN_BELT);
                earned.add(YELLOW_BELT);
                break;
            case GREEN:
                earned.add(GREEN_BELT);
                earned.add(YELLOW_BELT);
                break;
            case YELLOW:
                earned.add(YELLOW_BELT);
                break;
            case WHITE:
            default:
                break;
        }
        return earned;
    }
}
