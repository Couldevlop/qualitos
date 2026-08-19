package com.openlab.qualitos.quality.risk;

/**
 * Traduit un triplet sévérité / occurrence / détection en priorité d'action.
 *
 * <p>Trois bandes par note, donc vingt-sept combinaisons, écrites en toutes lettres
 * dans {@link #MATRIX} plutôt que dérivées d'une formule : une formule se relit mal
 * et se discute mal avec un ingénieur qualité, alors qu'une table se pointe du doigt.
 *
 * <p>Bandes retenues — sévérité : 9-10 haute, 5-8 moyenne, 1-4 basse ;
 * occurrence : 6-10 haute, 3-5 moyenne, 1-2 basse ;
 * détection : 7-10 mauvaise, 4-6 moyenne, 1-3 bonne (une note de détection élevée
 * signifie qu'on détecte MAL — c'est l'inverse de l'intuition, et l'erreur classique).
 */
public final class ActionPriorityCalculator {

    private ActionPriorityCalculator() {}

    /** [bande sévérité][bande occurrence][bande détection], bandes ordonnées haute → basse. */
    private static final ActionPriority[][][] MATRIX = {
            {   // sévérité haute : jamais LOW, une défaillance grave se traite
                    {ActionPriority.HIGH,   ActionPriority.HIGH,   ActionPriority.HIGH},
                    {ActionPriority.HIGH,   ActionPriority.HIGH,   ActionPriority.HIGH},
                    {ActionPriority.HIGH,   ActionPriority.MEDIUM, ActionPriority.MEDIUM}
            },
            {   // sévérité moyenne
                    {ActionPriority.HIGH,   ActionPriority.HIGH,   ActionPriority.HIGH},
                    {ActionPriority.HIGH,   ActionPriority.MEDIUM, ActionPriority.MEDIUM},
                    {ActionPriority.MEDIUM, ActionPriority.MEDIUM, ActionPriority.LOW}
            },
            {   // sévérité basse
                    {ActionPriority.MEDIUM, ActionPriority.MEDIUM, ActionPriority.MEDIUM},
                    {ActionPriority.MEDIUM, ActionPriority.LOW,    ActionPriority.LOW},
                    {ActionPriority.MEDIUM, ActionPriority.LOW,    ActionPriority.LOW}
            }
    };

    public static ActionPriority of(int severity, int occurrence, int detection) {
        return MATRIX[severityBand(rating(severity, "severity"))]
                     [occurrenceBand(rating(occurrence, "occurrence"))]
                     [detectionBand(rating(detection, "detection"))];
    }

    private static int rating(int value, String name) {
        if (value < 1 || value > 10) {
            throw new IllegalArgumentException("FMEA " + name + " must be between 1 and 10: " + value);
        }
        return value;
    }

    private static int severityBand(int s)   { return s >= 9 ? 0 : s >= 5 ? 1 : 2; }
    private static int occurrenceBand(int o) { return o >= 6 ? 0 : o >= 3 ? 1 : 2; }
    private static int detectionBand(int d)  { return d >= 7 ? 0 : d >= 4 ? 1 : 2; }
}
