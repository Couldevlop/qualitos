package com.openlab.qualitos.quality.consent.application;

import com.openlab.qualitos.quality.consent.domain.Consent;

public interface ConsentEventPublisher {

    enum Action { GRANTED, WITHDRAWN, EXPIRED }

    void publish(Consent consent, Action action);

    /** No-op pour tests / contextes où l'audit log est désactivé. */
    final class NoOp implements ConsentEventPublisher {
        @Override public void publish(Consent c, Action a) { }
    }
}
