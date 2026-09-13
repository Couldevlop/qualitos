package com.openlab.qualitos.quality.apqp;

/**
 * Contenu de livrable refusé : une forme incompatible avec son genre, ou un
 * renvoi qui ne désigne aucun enregistrement du client.
 *
 * <p>422 et non 400 : la requête est bien formée, c'est son CONTENU qui ne tient
 * pas debout pour ce livrable-là.
 */
public class ApqpDeliverableValidationException extends RuntimeException {

    public ApqpDeliverableValidationException(String message) {
        super(message);
    }
}
