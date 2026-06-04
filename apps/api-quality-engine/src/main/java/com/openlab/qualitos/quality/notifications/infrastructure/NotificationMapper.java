package com.openlab.qualitos.quality.notifications.infrastructure;

import com.openlab.qualitos.quality.notifications.domain.Notification;

/** Conversion domaine ⇄ entité JPA. */
final class NotificationMapper {

    private NotificationMapper() {}

    static NotificationJpaEntity toEntity(Notification n) {
        return new NotificationJpaEntity(n.getId(), n.getTenantId(), n.getRecipientUserId(),
                n.getType(), n.getTitle(), n.getBody(), n.getLink(), n.getCreatedAt(), n.getReadAt());
    }

    static Notification toDomain(NotificationJpaEntity e) {
        return new Notification(e.getId(), e.getTenantId(), e.getRecipientUserId(), e.getType(),
                e.getTitle(), e.getBody(), e.getLink(), e.getCreatedAt(), e.getReadAt());
    }
}
