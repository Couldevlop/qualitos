package com.openlab.qualitos.quality.apqp;

import java.util.UUID;

/** Pièce introuvable, ou versée sur un autre livrable — 404 dans les deux cas. */
public class ApqpDeliverableEvidenceNotFoundException extends RuntimeException {

    public ApqpDeliverableEvidenceNotFoundException(UUID id) {
        super("APQP deliverable evidence " + id + " not found");
    }
}
