package com.openlab.qualitos.quality.apqp;

/**
 * Plafond atteint : par fichier, par livrable, ou pour le cycle entier.
 *
 * <p>413 et non 400 : la requête est valide, c'est sa TAILLE qui est refusée, et
 * le message dit lequel des trois plafonds a parlé — sans quoi l'utilisateur
 * réessaie avec un fichier plus petit alors que c'est le cinquième.
 */
public class ApqpDeliverableEvidenceTooLargeException extends RuntimeException {

    public ApqpDeliverableEvidenceTooLargeException(String message) {
        super(message);
    }
}
