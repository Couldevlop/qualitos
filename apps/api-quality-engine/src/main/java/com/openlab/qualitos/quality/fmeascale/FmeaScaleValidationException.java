package com.openlab.qualitos.quality.fmeascale;

/**
 * Barème refusé : trou dans l'échelle, doublon de score, intitulé vide ou texte
 * trop long.
 *
 * <p>Nommée plutôt que générique, pour que l'écran puisse dire CE QUI manque.
 * « Requête invalide » devant un barème de dix lignes oblige à les relire une
 * par une.
 */
public class FmeaScaleValidationException extends RuntimeException {

    public FmeaScaleValidationException(String message) {
        super(message);
    }
}
