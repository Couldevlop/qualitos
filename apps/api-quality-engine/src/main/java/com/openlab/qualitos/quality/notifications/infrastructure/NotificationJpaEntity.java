package com.openlab.qualitos.quality.notifications.infrastructure;

import com.openlab.qualitos.quality.notifications.domain.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Mapping JPA des notifications (table V69 `notifications`). */
@Entity
@Table(name = "notifications")
public class NotificationJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "recipient_user_id", length = 128)
    private String recipientUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 16)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", columnDefinition = "TEXT")
    @org.hibernate.annotations.JdbcTypeCode(java.sql.Types.LONGVARCHAR)
    private String body;

    @Column(name = "link", length = 2048)
    private String link;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected NotificationJpaEntity() {
        // JPA
    }

    public NotificationJpaEntity(UUID id, UUID tenantId, String recipientUserId, NotificationType type,
                                 String title, String body, String link, Instant createdAt, Instant readAt) {
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

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getRecipientUserId() { return recipientUserId; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public String getLink() { return link; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }

    public void setReadAt(Instant readAt) { this.readAt = readAt; }
}
