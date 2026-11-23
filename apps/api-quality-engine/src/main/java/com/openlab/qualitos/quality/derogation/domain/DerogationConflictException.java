package com.openlab.qualitos.quality.derogation.domain;

/**
 * Deux gestes ont visé le même dossier en même temps, et le second a perdu.
 *
 * <p>Le cas n'est pas théorique : l'initiateur retire sa demande pendant que le
 * manager signe, ou le manager rouvre pendant que l'initiateur envoie au client.
 * Elle existe pour que l'écran dise « le dossier a changé, rechargez-le » plutôt
 * que de laisser remonter une panne technique.
 */
public class DerogationConflictException extends RuntimeException {

    public DerogationConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
