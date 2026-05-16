package com.openlab.qualitos.quality.apikeys.application;

import com.openlab.qualitos.quality.apikeys.domain.ApiKey;

/**
 * Port — publication d'événements vers audit log (OWASP A09 — security logging).
 */
public interface ApiKeyEventPublisher {

    enum Action { ISSUED, ROTATED, REVOKED, EXPIRED, USED }

    void publish(ApiKey key, Action action);

    final class NoOp implements ApiKeyEventPublisher {
        @Override public void publish(ApiKey k, Action a) { }
    }
}
