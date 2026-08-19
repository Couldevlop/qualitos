package com.openlab.qualitos.quality.risk;

/**
 * Priorité d'action AIAG-VDA : ce qu'il faut faire de la ligne, pas un score.
 *
 * <p>{@code HIGH} exige une action ou une justification écrite de l'absence
 * d'action ; {@code MEDIUM} demande d'étudier une action ; {@code LOW} laisse
 * l'action au choix de l'équipe.
 */
public enum ActionPriority {
    HIGH,
    MEDIUM,
    LOW
}
