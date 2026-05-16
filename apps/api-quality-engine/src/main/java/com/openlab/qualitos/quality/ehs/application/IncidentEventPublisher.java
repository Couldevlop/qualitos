package com.openlab.qualitos.quality.ehs.application;

import com.openlab.qualitos.quality.ehs.domain.Incident;

/**
 * Port — publication d'événements EHS vers un journal d'audit / bus. L'adapter
 * vit en infrastructure et peut router vers le journal d'audit, des webhooks,
 * Kafka… sans que le domaine/application ait connaissance du destinataire.
 *
 * Les implémentations DOIVENT être idempotentes au sens : appel plusieurs fois
 * pour la même transition d'un incident ne casse pas le système (le journal
 * d'audit, lui, est append-only — le seq number diverge mais c'est attendu).
 */
public interface IncidentEventPublisher {

    enum Action {
        REPORTED, INVESTIGATING, MITIGATED, CLOSED, CANCELLED,
        CAPA_LINKED, NC_LINKED, EDITED
    }

    void publish(Incident incident, Action action);

    /** Implémentation no-op pour tests / contextes où l'audit est désactivé. */
    final class NoOp implements IncidentEventPublisher {
        @Override public void publish(Incident incident, Action action) { /* no-op */ }
    }
}
