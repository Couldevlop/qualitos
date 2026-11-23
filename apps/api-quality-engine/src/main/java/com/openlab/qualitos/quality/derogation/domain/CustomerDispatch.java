package com.openlab.qualitos.quality.derogation.domain;

import java.time.Instant;

/**
 * L'envoi au client : les deux champs qui s'ajoutent au moment de « Validation
 * client », et la langue du courrier.
 *
 * @param language retenue sur le dossier : le PDF n'étant pas scellé il peut
 *                 être réémis, et sans cela un second envoi partirait dans une
 *                 autre langue sans que personne l'ait décidé
 * @param sentAt   la date d'envoi, et surtout la preuve QUE c'est parti
 */
public record CustomerDispatch(
        String customerEmail,
        String comment,
        DerogationLanguage language,
        Instant sentAt
) {
    public CustomerDispatch {
        customerEmail = Texte.net(customerEmail);
        comment = Texte.net(comment);
    }

    /** Rien n'est parti. */
    public static CustomerDispatch aucun() {
        return new CustomerDispatch(null, null, null, null);
    }

    public boolean estParti() {
        return sentAt != null;
    }
}
