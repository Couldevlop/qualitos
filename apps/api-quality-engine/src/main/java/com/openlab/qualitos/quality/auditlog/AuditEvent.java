package com.openlab.qualitos.quality.auditlog;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Événement d'audit immuable. Une seule colonne {@code blockchain_tx_ref} est
 * écrite après création (ancrage off-thread). Tout le reste est figé.
 *
 * Le hash {@code integrityHash} couvre la ligne courante ET le précédent hash
 * → la chaîne complète peut être vérifiée a posteriori (cf. {@link AuditEventService}).
 * La séquence {@code sequenceNo} est strictement croissante par tenant — toute
 * insertion concurrente sur le même tenant doit passer par
 * {@link AuditEventCounter} (verrou pessimiste) pour garantir la monotonicité.
 */
@Entity
@Table(name = "audit_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_audit_event_tenant_seq",
                        columnNames = {"tenant_id", "sequence_no"}),
                @UniqueConstraint(name = "uk_audit_event_hash",
                        columnNames = "integrity_hash")
        },
        indexes = {
                @Index(name = "idx_audit_event_tenant_seq",
                        columnList = "tenant_id, sequence_no"),
                @Index(name = "idx_audit_event_tenant_occurred",
                        columnList = "tenant_id, occurred_at"),
                @Index(name = "idx_audit_event_tenant_resource",
                        columnList = "tenant_id, resource_type, resource_id"),
                @Index(name = "idx_audit_event_tenant_actor",
                        columnList = "tenant_id, actor_user_id"),
                @Index(name = "idx_audit_event_tenant_action",
                        columnList = "tenant_id, action")
        })
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "sequence_no", nullable = false)
    private long sequenceNo;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 32, updatable = false)
    private ActorType actorType;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Column(nullable = false, length = 100, updatable = false)
    private String action;

    @Column(name = "resource_type", nullable = false, length = 64, updatable = false)
    private String resourceType;

    @Column(name = "resource_id", updatable = false)
    private UUID resourceId;

    @Column(length = 500, updatable = false)
    private String summary;

    @Lob
    @Column(name = "payload_json", updatable = false)
    private String payloadJson;

    @Column(name = "ip_address", length = 64, updatable = false)
    private String ipAddress;

    @Column(name = "user_agent", length = 500, updatable = false)
    private String userAgent;

    /** Hex SHA-256 de la ligne courante chaînée au {@code previousHash}. */
    @Column(name = "integrity_hash", nullable = false, length = 64, updatable = false)
    private String integrityHash;

    /** Hash de l'événement précédent (même tenant) ou null pour le premier. */
    @Column(name = "previous_hash", length = 64, updatable = false)
    private String previousHash;

    /** Référence transaction blockchain — null tant que pas ancré. */
    @Column(name = "blockchain_tx_ref", length = 200)
    private String blockchainTxRef;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public long getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(long sequenceNo) { this.sequenceNo = sequenceNo; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
    public ActorType getActorType() { return actorType; }
    public void setActorType(ActorType actorType) { this.actorType = actorType; }
    public UUID getActorUserId() { return actorUserId; }
    public void setActorUserId(UUID actorUserId) { this.actorUserId = actorUserId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public UUID getResourceId() { return resourceId; }
    public void setResourceId(UUID resourceId) { this.resourceId = resourceId; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getIntegrityHash() { return integrityHash; }
    public void setIntegrityHash(String integrityHash) { this.integrityHash = integrityHash; }
    public String getPreviousHash() { return previousHash; }
    public void setPreviousHash(String previousHash) { this.previousHash = previousHash; }
    public String getBlockchainTxRef() { return blockchainTxRef; }
    public void setBlockchainTxRef(String blockchainTxRef) { this.blockchainTxRef = blockchainTxRef; }
}
