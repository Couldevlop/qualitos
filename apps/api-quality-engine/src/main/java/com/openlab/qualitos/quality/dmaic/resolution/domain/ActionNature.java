package com.openlab.qualitos.quality.dmaic.resolution.domain;

/**
 * La nature d'une action de la fiche, telle que le dossier CAPA la porte.
 *
 * <p>Recopiée sur la ligne et non lue dans le dossier à chaque fois : le rapport
 * client doit rester lisible des années plus tard, y compris si le dossier a été
 * requalifié depuis. Les mêmes valeurs que {@code CapaActionType}, déclarées ici
 * pour que la fiche ne dépende pas du module CAPA.
 */
public enum ActionNature {

    /** Contient l'effet sans toucher à la cause : trier un lot, arrêter une ligne. */
    CONTAINMENT,

    /** Supprime la cause d'un écart survenu. */
    CORRECTIVE,

    /** Supprime la cause d'un écart qui n'est pas encore survenu. */
    PREVENTIVE
}
