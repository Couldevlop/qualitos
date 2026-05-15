package com.openlab.qualitos.quality.complaints;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Réponse envoyée au client (ou échange interne marqué "interne"). Préserve
 * l'historique conversationnel sans dépendre d'un système externe.
 */
@Entity
@Table(name = "complaint_responses",
        indexes = {
                @Index(name = "idx_complaint_response_complaint",
                        columnList = "complaint_id, sent_at"),
                @Index(name = "idx_complaint_response_tenant",
                        columnList = "tenant_id")
        })
public class ComplaintResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "complaint_id", nullable = false)
    private UUID complaintId;

    @Column(name = "author_user_id", nullable = false)
    private UUID authorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ComplaintChannel channel;

    @Column(nullable = false, length = 4000)
    private String body;

    /** true = note interne non envoyée au client (gardée pour audit). */
    @Column(name = "internal_note", nullable = false)
    private boolean internalNote;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (sentAt == null) sentAt = now;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getComplaintId() { return complaintId; }
    public void setComplaintId(UUID complaintId) { this.complaintId = complaintId; }
    public UUID getAuthorUserId() { return authorUserId; }
    public void setAuthorUserId(UUID authorUserId) { this.authorUserId = authorUserId; }
    public ComplaintChannel getChannel() { return channel; }
    public void setChannel(ComplaintChannel channel) { this.channel = channel; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public boolean isInternalNote() { return internalNote; }
    public void setInternalNote(boolean internalNote) { this.internalNote = internalNote; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
