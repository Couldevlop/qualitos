package com.openlab.qualitos.quality.derogation.application;

import com.openlab.qualitos.quality.derogation.domain.Derogation;

/**
 * Chaque geste du processus laisse sa ligne.
 *
 * <p><b>Un état qui écrase le précédent ne dit pas l'histoire.</b> La boucle
 * « À modifier » peut tourner plusieurs fois avant la signature, et la seule
 * colonne de statut ne saurait répondre à « combien d'allers-retours ». Chaque
 * demande, chaque renvoi motivé, chaque signature est donc un événement
 * journalisé.
 */
public interface DerogationAuditPort {

    enum Action {
        /** Le dossier est créé, au brouillon. */
        CREATED,
        /** La fiche est modifiée. */
        UPDATED,
        /** La validation manager est demandée. */
        APPROVAL_REQUESTED,
        /** L'initiateur retire sa demande. */
        WITHDRAWN,
        /** Le manager renvoie pour correction, avec son motif. */
        SENT_BACK,
        /** Le manager signe. */
        APPROVED,
        /** La signature est annulée, le dossier revient au brouillon. */
        REOPENED,
        /** Le courriel est parti au client, la dérogation en pièce jointe. */
        SENT_TO_CUSTOMER,
        /** La réponse du client est portée au dossier. */
        CUSTOMER_DECIDED
    }

    void record(Derogation dossier, Action action);
}
