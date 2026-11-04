package com.openlab.qualitos.quality.stakeholders.domain;

/**
 * Ce qui rend une exigence opposable : un contrat, un texte, une norme, un
 * engagement volontaire, ou une règle interne.
 *
 * <p>Deux exigences de même libellé ne se traitent pas de la même façon selon
 * ce caractère : manquer à une exigence réglementaire expose à une sanction,
 * manquer à un engagement volontaire expose à une perte de confiance.
 */
public enum MandatoryNature {
    CONTRACTUAL,
    REGULATORY,
    NORMATIVE,
    VOLUNTARY,
    INTERNAL
}
