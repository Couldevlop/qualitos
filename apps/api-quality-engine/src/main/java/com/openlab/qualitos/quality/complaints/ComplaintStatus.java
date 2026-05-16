package com.openlab.qualitos.quality.complaints;

/**
 * Lifecycle d'une réclamation client (§4.9).
 *
 * RECEIVED              : enregistrée, en attente de triage
 * UNDER_INVESTIGATION   : prise en charge, analyse en cours
 * RESPONDED             : réponse envoyée au client (préalable à RESOLVED)
 * RESOLVED              : action terminée, en attente de confirmation
 * CLOSED                : terminale — fermée
 * REJECTED              : terminale — rejetée (hors périmètre / spam)
 * REOPENED              : ré-ouverte après CLOSED ; revient à UNDER_INVESTIGATION
 */
public enum ComplaintStatus {
    RECEIVED,
    UNDER_INVESTIGATION,
    RESPONDED,
    RESOLVED,
    CLOSED,
    REJECTED,
    REOPENED
}
