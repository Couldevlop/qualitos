package com.openlab.qualitos.quality.dmaic.resolution.domain;

/**
 * Les rubriques de la définition du problème (Quoi, Qui, Pourquoi, Où, Quand,
 * Combien). Le rapport client les reprend une à une : une question de la phase
 * « Définir » sans rubrique n'y figurerait pas.
 */
public enum DefineTopic {
    WHAT,
    WHO,
    WHY,
    WHERE,
    WHEN,
    HOW_MANY
}
