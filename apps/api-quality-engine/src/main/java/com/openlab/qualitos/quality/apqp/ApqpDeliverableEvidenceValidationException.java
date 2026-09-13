package com.openlab.qualitos.quality.apqp;

/** Pièce refusée : type hors liste blanche, contenu vide, ou octets incohérents. */
public class ApqpDeliverableEvidenceValidationException extends RuntimeException {

    public ApqpDeliverableEvidenceValidationException(String message) {
        super(message);
    }
}
