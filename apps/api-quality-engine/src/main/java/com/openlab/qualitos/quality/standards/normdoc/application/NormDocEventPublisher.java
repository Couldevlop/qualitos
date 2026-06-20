package com.openlab.qualitos.quality.standards.normdoc.application;

import com.openlab.qualitos.quality.standards.normdoc.domain.NormativeDocument;

/**
 * Port d'audit : journalise les transitions d'un document normatif (OWASP A09).
 * L'adapter route vers le journal d'audit chaîné/ancrable.
 */
public interface NormDocEventPublisher {

    enum Action { GENERATED, EDITED, SUBMITTED, APPROVED, REJECTED, DELETED }

    void publish(NormativeDocument doc, Action action);

    /** Implémentation neutre (tests, contexte sans audit). */
    final class NoOp implements NormDocEventPublisher {
        @Override public void publish(NormativeDocument doc, Action action) { }
    }
}
