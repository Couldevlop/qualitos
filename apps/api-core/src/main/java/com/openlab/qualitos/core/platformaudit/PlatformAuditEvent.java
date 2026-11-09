package com.openlab.qualitos.core.platformaudit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Un acte de l'éditeur sur le dossier d'un client, consigné pour de bon.
 *
 * <p><b>Immuable jusque dans la classe.</b> Aucune propriété n'a de mutateur,
 * et toutes les colonnes sont {@code updatable = false}. Ce n'est pas de la
 * coquetterie : la V12 pose un déclencheur qui refuse tout {@code UPDATE}, et
 * du code qui tenterait quand même une modification échouerait au {@code
 * flush} — loin de la ligne fautive, avec une erreur SQL illisible. Interdire
 * le geste ici le fait échouer à la compilation.
 *
 * <p>Contrairement à l'événement d'audit du moteur de qualité, il n'y a pas de
 * colonne d'ancrage à renseigner après coup : rien, ici, ne s'écrit après
 * l'insertion.
 *
 * <p>L'empreinte est posée en une fois par {@link #seal(String)}, parce
 * qu'elle se calcule sur les autres propriétés et ne peut donc pas être connue
 * du constructeur.
 */
@Entity
@Table(name = "platform_audit_events")
public class PlatformAuditEvent {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sequence_no", nullable = false, updatable = false)
    private long sequenceNo;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    private Instant recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 32, updatable = false)
    private PlatformActorType actorType;

    @Column(name = "actor_user_id", updatable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 64, updatable = false)
    private PlatformAuditAction action;

    @Column(name = "target_type", nullable = false, length = 64, updatable = false)
    private String targetType;

    @Column(name = "target_id", updatable = false)
    private UUID targetId;

    @Column(name = "target_tenant_id", updatable = false)
    private UUID targetTenantId;

    @Column(name = "summary", nullable = false, length = 512, updatable = false)
    private String summary;

    @Column(name = "payload_json", updatable = false)
    private String payloadJson;

    @Column(name = "ip_address", length = 64, updatable = false)
    private String ipAddress;

    @Column(name = "user_agent", length = 256, updatable = false)
    private String userAgent;

    @Column(name = "integrity_hash", nullable = false, length = 64, updatable = false)
    private String integrityHash;

    @Column(name = "previous_hash", length = 64, updatable = false)
    private String previousHash;

    /** Pour JPA seul. */
    protected PlatformAuditEvent() {
    }

    PlatformAuditEvent(UUID id, long sequenceNo, Instant occurredAt, Instant recordedAt,
                       PlatformActorType actorType, UUID actorUserId,
                       PlatformAuditAction action, String targetType, UUID targetId,
                       UUID targetTenantId, String summary, String payloadJson,
                       String ipAddress, String userAgent, String previousHash) {
        this.id = id;
        this.sequenceNo = sequenceNo;
        this.occurredAt = occurredAt;
        this.recordedAt = recordedAt;
        this.actorType = actorType;
        this.actorUserId = actorUserId;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetTenantId = targetTenantId;
        this.summary = summary;
        this.payloadJson = payloadJson;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.previousHash = previousHash;
    }

    /**
     * Pose l'empreinte, une seule fois.
     *
     * <p>Le second appel lève : une empreinte recalculée sur place est
     * exactement le geste par lequel on maquillerait une ligne tout en
     * laissant la vérification de chaîne retomber sur ses pieds.
     */
    void seal(String hash) {
        if (this.integrityHash != null) {
            throw new IllegalStateException(
                    "L'empreinte d'un evenement d'audit ne se pose qu'une fois (evenement " + id + ")");
        }
        this.integrityHash = hash;
    }

    public UUID getId() {
        return id;
    }

    public long getSequenceNo() {
        return sequenceNo;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public PlatformActorType getActorType() {
        return actorType;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public PlatformAuditAction getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public UUID getTargetId() {
        return targetId;
    }

    public UUID getTargetTenantId() {
        return targetTenantId;
    }

    public String getSummary() {
        return summary;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getIntegrityHash() {
        return integrityHash;
    }

    public String getPreviousHash() {
        return previousHash;
    }
}
