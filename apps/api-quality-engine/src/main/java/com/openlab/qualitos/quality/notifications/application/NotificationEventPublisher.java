package com.openlab.qualitos.quality.notifications.application;

import com.openlab.qualitos.quality.notifications.domain.Notification;

/** Port d'audit (OWASP A09) : trace la création d'une notification. */
public interface NotificationEventPublisher {

    void created(Notification notification);

    /** Implémentation par défaut sans effet (tests, contextes sans audit). */
    final class NoOp implements NotificationEventPublisher {
        @Override
        public void created(Notification notification) {
            // intentionnellement vide
        }
    }
}
