package com.openlab.qualitos.quality.risk;

/**
 * Classement d'une caractéristique du produit ou du procédé.
 *
 * <p>L'IATF impose de repérer les caractéristiques spéciales — celles dont la
 * dérive touche la sécurité ou une exigence réglementaire — parce qu'elles
 * rendent le contrôle obligatoire dans le control plan, quel que soit le RPN.
 */
public enum CharacteristicClass {
    STANDARD,
    SPECIAL,
    SAFETY,
    REGULATORY
}
