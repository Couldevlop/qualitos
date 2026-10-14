package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.change.domain.MocHistoryEvent;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Une ligne de l'historique d'une demande : quoi, quand, qui, et le détail
 * (rôle, commentaire) tel qu'il était. Jamais modifiée après écriture.
 */
@Entity
@Table(name = "change_history",
        indexes = @Index(name = "ix_change_history_change", columnList = "change_id, occurred_at"))
public class ChangeHistoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "change_id", nullable = false)
    private UUID changeId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MocHistoryEvent event;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "actor_name", length = 200)
    private String actorName;

    @Column(length = 1000)
    private String detail;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getChangeId() { return changeId; }
    public void setChangeId(UUID changeId) { this.changeId = changeId; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public MocHistoryEvent getEvent() { return event; }
    public void setEvent(MocHistoryEvent event) { this.event = event; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public String getActorName() { return actorName; }
    public void setActorName(String actorName) { this.actorName = actorName; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
}
