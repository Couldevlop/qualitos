package com.openlab.qualitos.quality.nonconformity;

public enum NcStatus {

    OPEN,
    UNDER_ANALYSIS,
    ACTION_DEFINED,
    RESOLVED,
    CLOSED,

    /** Le constat n'avait pas lieu d'être : on le retire. */
    CANCELLED,

    /**
     * Le constat a été examiné et écarté — réservé aux écarts signalés du DEHORS.
     *
     * <p>Distinct de {@code CANCELLED}, et pas par souci de nuance : annuler dit
     * « ce constat n'avait pas lieu d'être », rejeter dit « il a été instruit,
     * voici pourquoi il n'est pas retenu ». C'est la seconde phrase qu'un client
     * ou un auditeur vient lire, et le motif l'accompagne toujours.
     */
    REJECTED
}
