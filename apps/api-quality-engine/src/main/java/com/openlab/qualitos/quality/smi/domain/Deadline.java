package com.openlab.qualitos.quality.smi.domain;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Une échéance de « À traiter cette semaine » : ce qu'il faut faire, d'où ça
 * vient, et pour quand.
 *
 * @param targetId l'objet vers lequel l'écran renvoie (dossier CAPA,
 *                 équipement, changement, risque, audit)
 */
public record Deadline(Kind kind, UUID targetId, String reference, String title, LocalDate dueOn) {

    public enum Kind {
        CAPA_ACTION,
        CALIBRATION,
        CHANGE,
        RISK_REVIEW,
        AUDIT
    }
}
