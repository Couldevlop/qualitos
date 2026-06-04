package com.openlab.qualitos.quality.notifications.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification in-app (modèle de domaine pur — aucune dépendance framework).
 *
 * <p>Destinée à un utilisateur ({@code recipientUserId}) ou diffusée à tout le tenant
 * ({@code recipientUserId == null}). L'isolation tenant est une invariante portée par les
 * couches application/infrastructure (OWASP A01).
 */
public final class Notification {

    private final UUID id;
    private final UUID tenantId;
    private final String recipientUserId; // null = diffusion à tout le tenant
    private final NotificationType type;
    private final String title;
    private final String body;
    private final String link;
    private final Instant createdAt;
    private Instant readAt;

    public Notification(UUID id, UUID tenantId, String recipientUserId, NotificationType type,
                        String title, String body, String link, Instant createdAt, Instant readAt) {
        if (id == null || tenantId == null || type == null) {
            throw new IllegalArgumentException("id, tenantId et type sont requis");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title requis");
        }
        this.id = id;
        this.tenantId = tenantId;
        this.recipientUserId = recipientUserId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.link = link;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    /** Crée une notification non lue. */
    public static Notification create(UUID id, UUID tenantId, String recipientUserId,
                                      NotificationType type, String title, String body,
                                      String link, Instant createdAt) {
        return new Notification(id, tenantId, recipientUserId, type, title, body, link, createdAt, null);
    }

    /** Marque comme lue (idempotent : ne réécrit pas la date si déjà lue). */
    public void markRead(Instant at) {
        if (this.readAt == null) {
            this.readAt = at;
        }
    }

    public boolean isRead() {
        return readAt != null;
    }

    /** Visible par {@code userId} : destinataire explicite ou diffusion tenant. */
    public boolean isVisibleTo(String userId) {
        return recipientUserId == null || recipientUserId.equals(userId);
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getRecipientUserId() { return recipientUserId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getLink() { return link; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
}
