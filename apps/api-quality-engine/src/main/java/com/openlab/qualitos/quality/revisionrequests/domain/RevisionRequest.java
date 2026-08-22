package com.openlab.qualitos.quality.revisionrequests.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Agrégat — une révision proposée à un document approuvé.
 *
 * <p>Le PFMEA et le control plan ne se mettent pas à jour tout seuls après une
 * non-conformité : un document opposable qui bougerait sans décision humaine
 * serait un écart en audit de certification. Ils reçoivent une proposition
 * chiffrée et justifiée, qu'un humain accepte ou refuse.
 *
 * <p>Le refus exige une note. « On n'a pas bougé » est une décision qualité comme
 * une autre, et l'auditeur veut lire pourquoi.
 */
public final class RevisionRequest {

    private UUID id;
    private final UUID tenantId;
    private final UUID productId;
    private final RevisionTargetType targetType;
    private final UUID targetId;
    private final RevisionTriggerType triggerType;
    private final UUID triggerRefId;
    private final String triggerRefLabel;
    private final String rationale;
    private final ProposedChange change;
    private RevisionRequestStatus status;
    private UUID decidedBy;
    private Instant decidedAt;
    private String decisionNote;
    private final Instant createdAt;
    private Instant updatedAt;

    private RevisionRequest(UUID tenantId, UUID productId, RevisionTargetType targetType,
                            UUID targetId, RevisionTriggerType triggerType, UUID triggerRefId,
                            String triggerRefLabel, String rationale, ProposedChange change,
                            Instant now) {
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.productId = Objects.requireNonNull(productId, "productId");
        this.targetType = Objects.requireNonNull(targetType, "targetType");
        this.triggerType = Objects.requireNonNull(triggerType, "triggerType");
        this.triggerRefId = Objects.requireNonNull(triggerRefId, "triggerRefId");
        this.triggerRefLabel = require(triggerRefLabel, "triggerRefLabel", 120);
        this.rationale = require(rationale, "rationale", 1000);
        this.change = Objects.requireNonNull(change, "change");
        this.createdAt = Objects.requireNonNull(now, "now");
        this.updatedAt = now;
        this.status = RevisionRequestStatus.PENDING;
        if (!targetType.isCreation() && targetId == null) {
            // Une modification sans cible ne pourrait jamais être appliquée : la
            // laisser passer produirait une demande morte que personne ne peut honorer.
            throw new IllegalArgumentException("A modification request needs a target: " + targetType);
        }
        this.targetId = targetId;
    }

    public static RevisionRequest propose(UUID tenantId, UUID productId,
                                          RevisionTargetType targetType, UUID targetId,
                                          RevisionTriggerType triggerType, UUID triggerRefId,
                                          String triggerRefLabel, String rationale,
                                          ProposedChange change, Instant now) {
        return new RevisionRequest(tenantId, productId, targetType, targetId, triggerType,
                triggerRefId, triggerRefLabel, rationale, change, now);
    }

    /** Reconstruction depuis la persistance : aucune transition rejouée. */
    public static RevisionRequest rehydrate(UUID id, UUID tenantId, UUID productId,
                                            RevisionTargetType targetType, UUID targetId,
                                            RevisionTriggerType triggerType, UUID triggerRefId,
                                            String triggerRefLabel, String rationale,
                                            ProposedChange change, RevisionRequestStatus status,
                                            UUID decidedBy, Instant decidedAt, String decisionNote,
                                            Instant createdAt, Instant updatedAt) {
        RevisionRequest request = new RevisionRequest(tenantId, productId, targetType, targetId,
                triggerType, triggerRefId, triggerRefLabel, rationale, change, createdAt);
        request.id = id;
        request.status = status == null ? RevisionRequestStatus.PENDING : status;
        request.decidedBy = decidedBy;
        request.decidedAt = decidedAt;
        request.decisionNote = decisionNote;
        request.updatedAt = updatedAt;
        return request;
    }

    private static String require(String value, String field, int max) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty() || trimmed.length() > max) {
            throw new IllegalArgumentException("Invalid revision request " + field);
        }
        return trimmed;
    }

    private void requirePending(String action) {
        if (status != RevisionRequestStatus.PENDING) {
            throw new RevisionRequestStateException(
                    "Cette demande est déjà " + status + " : impossible de l'" + action);
        }
    }

    public void accept(UUID by, Instant when) {
        requirePending("accepter");
        this.status = RevisionRequestStatus.ACCEPTED;
        this.decidedBy = Objects.requireNonNull(by, "by");
        this.decidedAt = Objects.requireNonNull(when, "when");
        this.updatedAt = when;
    }

    public void reject(UUID by, String note, Instant when) {
        requirePending("refuser");
        // L'auditeur veut lire POURQUOI on n'a pas bougé. Un refus muet est
        // exactement l'écart qu'il cherche.
        this.decisionNote = require(note, "decisionNote", 1000);
        this.status = RevisionRequestStatus.REJECTED;
        this.decidedBy = Objects.requireNonNull(by, "by");
        this.decidedAt = Objects.requireNonNull(when, "when");
        this.updatedAt = when;
    }

    /** Remplacée par une proposition plus récente sur la même cible. */
    public void supersede(Instant when) {
        requirePending("remplacer");
        this.status = RevisionRequestStatus.SUPERSEDED;
        this.updatedAt = Objects.requireNonNull(when, "when");
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getProductId() { return productId; }
    public RevisionTargetType getTargetType() { return targetType; }
    public UUID getTargetId() { return targetId; }
    public RevisionTriggerType getTriggerType() { return triggerType; }
    public UUID getTriggerRefId() { return triggerRefId; }
    public String getTriggerRefLabel() { return triggerRefLabel; }
    public String getRationale() { return rationale; }
    public ProposedChange getChange() { return change; }
    public RevisionRequestStatus getStatus() { return status; }
    public UUID getDecidedBy() { return decidedBy; }
    public Instant getDecidedAt() { return decidedAt; }
    public String getDecisionNote() { return decisionNote; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
