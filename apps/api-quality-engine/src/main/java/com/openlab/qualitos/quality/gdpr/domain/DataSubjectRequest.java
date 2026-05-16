package com.openlab.qualitos.quality.gdpr.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Demande RGPD d'une personne concernée (data subject).
 *
 * Sécurité / privacy by design (OWASP A04, A02) :
 *  - {@code subjectIdentifierHash} (SHA-256) — la PII en clair (email…)
 *    n'est JAMAIS persistée par l'agrégat.
 *  - {@code subjectIdentifierLabel} : libellé partiel non-sensitif pour
 *    affichage (ex "j***@gmail.com"). Saisi par l'application après masquage.
 *  - Délai légal {@code deadlineAt} = receivedAt + 30 jours (RGPD Art. 12.3),
 *    extensible une fois jusqu'à +60 jours.
 *  - Transitions encapsulées ; toute mutation post-terminale levée.
 */
public final class DataSubjectRequest {

    public static final Duration DEFAULT_RESPONSE_WINDOW = Duration.ofDays(30);
    public static final Duration MAX_EXTENDED_WINDOW     = Duration.ofDays(90); // 30 + 2×30

    private static final Map<SubjectRequestStatus, Set<SubjectRequestStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(SubjectRequestStatus.class);
        ALLOWED.put(SubjectRequestStatus.RECEIVED,    EnumSet.of(
                SubjectRequestStatus.IN_PROGRESS, SubjectRequestStatus.REJECTED));
        ALLOWED.put(SubjectRequestStatus.IN_PROGRESS, EnumSet.of(
                SubjectRequestStatus.COMPLETED, SubjectRequestStatus.REJECTED));
        ALLOWED.put(SubjectRequestStatus.COMPLETED,   EnumSet.noneOf(SubjectRequestStatus.class));
        ALLOWED.put(SubjectRequestStatus.REJECTED,    EnumSet.noneOf(SubjectRequestStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final SubjectRequestType type;
    private final String subjectIdentifierHash;
    private final String subjectIdentifierLabel;
    private SubjectRequestStatus status;
    private final Instant receivedAt;
    private Instant deadlineAt;
    private boolean extended;
    private Instant inProgressAt;
    private Instant completedAt;
    private String rejectionReason;
    private String resolutionNotes;
    private String evidenceUrl;
    private final UUID requestedByUserId;
    private UUID handledByUserId;
    private Instant updatedAt;

    public DataSubjectRequest(UUID id, UUID tenantId, SubjectRequestType type,
                              String subjectIdentifierHash, String subjectIdentifierLabel,
                              SubjectRequestStatus status, Instant receivedAt, Instant deadlineAt,
                              boolean extended, Instant inProgressAt, Instant completedAt,
                              String rejectionReason, String resolutionNotes, String evidenceUrl,
                              UUID requestedByUserId, UUID handledByUserId, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.type = Objects.requireNonNull(type, "type");
        this.subjectIdentifierHash = require(subjectIdentifierHash, "subjectIdentifierHash");
        this.subjectIdentifierLabel = subjectIdentifierLabel;
        this.status = Objects.requireNonNull(status, "status");
        this.receivedAt = Objects.requireNonNull(receivedAt, "receivedAt");
        this.deadlineAt = Objects.requireNonNull(deadlineAt, "deadlineAt");
        this.extended = extended;
        this.inProgressAt = inProgressAt;
        this.completedAt = completedAt;
        this.rejectionReason = rejectionReason;
        this.resolutionNotes = resolutionNotes;
        this.evidenceUrl = evidenceUrl;
        this.requestedByUserId = Objects.requireNonNull(requestedByUserId, "requestedByUserId");
        this.handledByUserId = handledByUserId;
        this.updatedAt = updatedAt != null ? updatedAt : receivedAt;
    }

    public static DataSubjectRequest receive(UUID tenantId, SubjectRequestType type,
                                             String subjectIdentifierHash,
                                             String subjectIdentifierLabel,
                                             UUID requestedByUserId, Instant now) {
        return new DataSubjectRequest(null, tenantId, type,
                subjectIdentifierHash, subjectIdentifierLabel,
                SubjectRequestStatus.RECEIVED, now, now.plus(DEFAULT_RESPONSE_WINDOW),
                false, null, null, null, null, null,
                requestedByUserId, null, now);
    }

    public void startProcessing(UUID handledByUserId, Instant now) {
        ensureTransition(SubjectRequestStatus.IN_PROGRESS);
        this.handledByUserId = handledByUserId;
        this.inProgressAt = now;
        this.status = SubjectRequestStatus.IN_PROGRESS;
        this.updatedAt = now;
    }

    public void complete(String resolutionNotes, String evidenceUrl,
                         UUID handledByUserId, Instant now) {
        ensureTransition(SubjectRequestStatus.COMPLETED);
        if (resolutionNotes == null || resolutionNotes.isBlank()) {
            throw new SubjectRequestStateException("resolutionNotes required to complete");
        }
        this.resolutionNotes = resolutionNotes;
        this.evidenceUrl = evidenceUrl;
        if (handledByUserId != null) this.handledByUserId = handledByUserId;
        this.completedAt = now;
        this.status = SubjectRequestStatus.COMPLETED;
        this.updatedAt = now;
    }

    public void reject(String reason, UUID handledByUserId, Instant now) {
        ensureTransition(SubjectRequestStatus.REJECTED);
        if (reason == null || reason.isBlank()) {
            throw new SubjectRequestStateException("rejection reason required");
        }
        this.rejectionReason = reason;
        if (handledByUserId != null) this.handledByUserId = handledByUserId;
        this.completedAt = now;
        this.status = SubjectRequestStatus.REJECTED;
        this.updatedAt = now;
    }

    /** RGPD Art. 12.3 — extension unique d'une fois maxi (jusqu'à +60 jours total). */
    public void extendDeadline(Instant newDeadline, Instant now) {
        if (isTerminal()) {
            throw new SubjectRequestStateException("Cannot extend a terminal request");
        }
        if (extended) {
            throw new SubjectRequestStateException("Deadline already extended once");
        }
        Instant maxAllowed = receivedAt.plus(MAX_EXTENDED_WINDOW);
        if (newDeadline == null || !newDeadline.isAfter(deadlineAt) || newDeadline.isAfter(maxAllowed)) {
            throw new SubjectRequestStateException(
                    "newDeadline must be > current deadline and ≤ receivedAt + 90 days");
        }
        this.deadlineAt = newDeadline;
        this.extended = true;
        this.updatedAt = now;
    }

    public boolean isOverdue(Instant ref) {
        return !isTerminal() && ref.isAfter(deadlineAt);
    }

    public boolean isTerminal() {
        return status == SubjectRequestStatus.COMPLETED || status == SubjectRequestStatus.REJECTED;
    }

    private void ensureTransition(SubjectRequestStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new SubjectRequestStateException(
                    "Transition " + status + " → " + target + " is not allowed");
        }
    }

    private static String require(String v, String f) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " required");
        return v;
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public SubjectRequestType getType() { return type; }
    public String getSubjectIdentifierHash() { return subjectIdentifierHash; }
    public String getSubjectIdentifierLabel() { return subjectIdentifierLabel; }
    public SubjectRequestStatus getStatus() { return status; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getDeadlineAt() { return deadlineAt; }
    public boolean isExtended() { return extended; }
    public Instant getInProgressAt() { return inProgressAt; }
    public Instant getCompletedAt() { return completedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public String getResolutionNotes() { return resolutionNotes; }
    public String getEvidenceUrl() { return evidenceUrl; }
    public UUID getRequestedByUserId() { return requestedByUserId; }
    public UUID getHandledByUserId() { return handledByUserId; }
    public Instant getUpdatedAt() { return updatedAt; }
}
