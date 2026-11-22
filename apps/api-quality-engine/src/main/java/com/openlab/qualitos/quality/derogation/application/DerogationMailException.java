package com.openlab.qualitos.quality.derogation.application;

/**
 * Le courriel au client n'est pas parti.
 *
 * <p>Elle existe pour que l'écran dise <i>pourquoi</i> plutôt que de montrer une
 * panne technique, et pour que le dossier reste renvoyable. Une messagerie non
 * configurée en est un cas : c'est un refus explicite, pas un silence — sinon le
 * dossier passerait « En attente client » sans qu'aucun client n'ait rien reçu.
 */
public class DerogationMailException extends RuntimeException {

    public DerogationMailException(String message) {
        super(message);
    }

    public DerogationMailException(String message, Throwable cause) {
        super(message, cause);
    }
}
