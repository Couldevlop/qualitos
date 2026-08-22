package com.openlab.qualitos.quality.capa.effectiveness.domain;

/**
 * Ce qu'on peut dire, ou non, de l'efficacité d'une CAPA close.
 *
 * <p>Trois états, et pas un taux dans tous les cas : un chiffre rendu quand il
 * n'a pas de sens est plus nuisible qu'une case vide, parce qu'il se moyenne, se
 * compare et finit dans une revue de direction.
 */
public enum MeasurementStatus {

    /**
     * Aucune occurrence avant l'ouverture du dossier. On ne mesure pas une
     * réduction à partir de zéro : annoncer 100 % reviendrait à féliciter une
     * action dont rien ne dit qu'elle servait à quelque chose.
     */
    NOT_MEASURABLE,

    /**
     * Fenêtre d'observation encore en cours. Le décompte des récidives est déjà
     * utile — « aucune en deux mois » se dit — mais le taux, lui, comparerait
     * une période partielle à une période entière et flatterait le résultat.
     */
    IN_OBSERVATION,

    /** Fenêtre écoulée et occurrences antérieures connues : le taux vaut. */
    MEASURED
}
