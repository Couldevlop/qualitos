package com.openlab.qualitos.quality.kafkarelay;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Curseur du relais Kafka : dernière séquence d'audit publiée par tenant (V67).
 * Outbox basé sur {@code audit_events} (append-only, séquence sans trou par tenant).
 */
@Entity
@Table(name = "kafka_relay_cursor")
public class KafkaRelayCursor {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "last_published_seq", nullable = false)
    private long lastPublishedSeq;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected KafkaRelayCursor() {
        // JPA
    }

    public KafkaRelayCursor(UUID tenantId, long lastPublishedSeq, Instant updatedAt) {
        this.tenantId = tenantId;
        this.lastPublishedSeq = lastPublishedSeq;
        this.updatedAt = updatedAt;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public long getLastPublishedSeq() {
        return lastPublishedSeq;
    }

    public void setLastPublishedSeq(long lastPublishedSeq) {
        this.lastPublishedSeq = lastPublishedSeq;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
