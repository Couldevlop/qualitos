package com.openlab.qualitos.quality.auditlog;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Compteur monotonique par tenant. Une ligne par tenant — récupérée en
 * {@link jakarta.persistence.LockModeType#PESSIMISTIC_WRITE} pour sérialiser
 * les inserts d'événements et garantir la séquence stricte.
 */
@Entity
@Table(name = "audit_event_counters")
public class AuditEventCounter {

    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "last_sequence_no", nullable = false)
    private long lastSequenceNo;

    public AuditEventCounter() {}

    public AuditEventCounter(UUID tenantId, long lastSequenceNo) {
        this.tenantId = tenantId;
        this.lastSequenceNo = lastSequenceNo;
    }

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public long getLastSequenceNo() { return lastSequenceNo; }
    public void setLastSequenceNo(long lastSequenceNo) { this.lastSequenceNo = lastSequenceNo; }
}
