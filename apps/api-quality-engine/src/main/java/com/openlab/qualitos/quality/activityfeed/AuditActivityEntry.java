package com.openlab.qualitos.quality.activityfeed;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entrée du read-model "flux d'activité" (V68), projetée depuis le topic Kafka
 * {@code qualitos.audit-events} par {@link AuditActivityFeedConsumer}. Unicité
 * (tenant_id, sequence_no) → idempotent face au at-least-once.
 */
@Entity
@Table(name = "audit_activity_feed")
public class AuditActivityEntry {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "sequence_no", nullable = false)
    private long sequenceNo;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "recorded_at")
    private Instant recordedAt;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "resource_type", length = 64)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "summary", length = 500)
    private String summary;

    protected AuditActivityEntry() {
        // JPA
    }

    public AuditActivityEntry(UUID id, UUID tenantId, long sequenceNo, Instant occurredAt,
                              Instant recordedAt, String action, String resourceType,
                              UUID resourceId, UUID actorUserId, String summary) {
        this.id = id;
        this.tenantId = tenantId;
        this.sequenceNo = sequenceNo;
        this.occurredAt = occurredAt;
        this.recordedAt = recordedAt;
        this.action = action;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.actorUserId = actorUserId;
        this.summary = summary;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public long getSequenceNo() { return sequenceNo; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getRecordedAt() { return recordedAt; }
    public String getAction() { return action; }
    public String getResourceType() { return resourceType; }
    public UUID getResourceId() { return resourceId; }
    public UUID getActorUserId() { return actorUserId; }
    public String getSummary() { return summary; }
}
