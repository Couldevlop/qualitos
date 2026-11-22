package com.openlab.qualitos.quality.derogation.application;

import com.openlab.qualitos.quality.derogation.domain.Derogation;

/**
 * Les notifications internes du processus : l'alerte rouge et son nombre, dans
 * « Mes validations » du manager et « Mes notifications » de l'initiateur.
 *
 * <p>Elles partent <b>toujours</b>, même quand la messagerie est coupée : le
 * courriel est un complément du signal, jamais sa condition.
 */
public interface DerogationNotificationPort {

    /** Au manager désigné : un dossier attend sa signature. */
    void approvalRequested(Derogation dossier);

    /** À l'initiateur : son dossier est signé, il peut l'envoyer au client. */
    void approved(Derogation dossier);

    /** À l'initiateur : son dossier revient, avec le motif à corriger. */
    void sentBack(Derogation dossier);
}
