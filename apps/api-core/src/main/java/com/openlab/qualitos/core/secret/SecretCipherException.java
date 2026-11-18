package com.openlab.qualitos.core.secret;

/**
 * Le secret n'a pas pu etre chiffre ou dechiffre.
 *
 * <p>Volontairement AVARE en details. Le message qui remonte dit que
 * l'operation a echoue, jamais pourquoi : distinguer « mauvaise cle » de
 * « cryptogramme altere » renseignerait un attaquant sur l'etat du coffre, et
 * ne renseignerait personne d'autre -- celui qui exploite lit le journal, pas
 * la reponse HTTP.
 */
public class SecretCipherException extends RuntimeException {

    public SecretCipherException(String message) {
        super(message);
    }

    public SecretCipherException(String message, Throwable cause) {
        super(message, cause);
    }
}
