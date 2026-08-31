package com.openlab.qualitos.quality.fmeascale;

/**
 * Les trois échelles sur lesquelles se cote un mode de défaillance.
 *
 * <p>Leur produit est le RPN. C'est pourquoi elles vont toutes de 1 à 10 et
 * pourquoi on ne peut pas en ajouter une quatrième sans changer le calcul du
 * risque : l'énumération est fermée, et le compilateur le rappelle à chaque
 * {@code switch}.
 */
public enum FmeaScaleKind {

    /** Gravité de l'effet pour le client. */
    SEVERITY,

    /** Fréquence attendue de la défaillance. */
    OCCURRENCE,

    /** Chance de repérer la défaillance avec les contrôles en place. */
    DETECTION
}
