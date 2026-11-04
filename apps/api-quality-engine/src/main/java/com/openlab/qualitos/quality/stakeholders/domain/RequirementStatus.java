package com.openlab.qualitos.quality.stakeholders.domain;

/**
 * Où en est le traitement d'une exigence.
 *
 * <p>{@code TO_REASSESS} est l'état qu'on atteint quand quelque chose a changé
 * — un avenant au contrat, une révision de norme — et qu'on ne peut plus
 * affirmer la maîtrise sans l'avoir revue. C'est le seul état qui se gagne par
 * un événement extérieur, et non par un travail accompli.
 */
public enum RequirementStatus {
    TO_ANALYSE,
    APPLICABLE,
    CONTROLLED,
    TO_REASSESS
}
