package com.openlab.qualitos.quality.objective;

/**
 * La vie de la DÉFINITION d'un objectif, à ne pas confondre avec
 * {@link ObjectiveStatus}, qui dit si l'objectif est tenu.
 *
 * <p>Un objectif ne se supprime pas une fois qu'une revue s'est appuyée dessus :
 * il s'archive. Effacer l'objectif d'un trimestre passé effacerait la raison des
 * CAPA qu'il a fait ouvrir, et une revue de direction relue l'année suivante ne
 * se comprendrait plus.
 */
public enum ObjectiveLifecycle {

    /** Suivi : il compte dans la revue, et sa règle peut proposer une CAPA. */
    ACTIVE,

    /** Retiré du suivi, conservé pour l'historique et les dossiers qu'il a ouverts. */
    ARCHIVED
}
