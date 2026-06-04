package com.openlab.qualitos.quality.notifications.application;

import com.openlab.qualitos.quality.notifications.domain.Notification;
import com.openlab.qualitos.quality.notifications.domain.NotificationType;

import java.time.Instant;
import java.util.UUID;

/** DTO d'application des notifications. */
public final class NotificationDto {

    private NotificationDto() {}

    /** Commande de création (le destinataire est optionnel = diffusion tenant). */
    public record CreateCommand(
            String recipientUserId,
            NotificationType type,
            String title,
            String body,
            String link
    ) {}

    /** Vue exposée (ne révèle pas le tenant). */
    public record View(
            UUID id,
            NotificationType type,
            String title,
            String body,
            String link,
            boolean read,
            Instant createdAt,
            Instant readAt
    ) {
        public static View from(Notification n) {
            return new View(n.getId(), n.getType(), n.getTitle(), n.getBody(), n.getLink(),
                    n.isRead(), n.getCreatedAt(), n.getReadAt());
        }
    }

    public record UnreadCount(long unread) {}
}
