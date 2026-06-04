package com.openlab.qualitos.quality.notifications.domain;

import java.util.UUID;

/** Notification absente ou hors du périmètre du tenant/utilisateur (mappée en 404). */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(UUID id) {
        super("Notification not found: " + id);
    }
}
