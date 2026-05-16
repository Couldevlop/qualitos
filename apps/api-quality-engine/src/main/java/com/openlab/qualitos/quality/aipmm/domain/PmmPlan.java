package com.openlab.qualitos.quality.aipmm.domain;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat — plan de surveillance post-marché d'un système IA (AI Act Art. 72).
 *
 * Le provider doit documenter : métriques surveillées, méthode de collecte
 * (logs, retours utilisateurs, audits), fréquence d'évaluation, responsables,
 * critères de déclenchement d'actions correctives, lien vers le QMS.
 *
 * Cycle de vie :
 *   DRAFT → ACTIVE → SUSPENDED → ACTIVE
 *   DRAFT|ACTIVE|SUSPENDED → CLOSED
 *
 * Garde-fous (dupliqués DB) :
 *  - activation exige metricsMonitored + collectionMethod + reviewFrequency
 *  - CLOSED exige effective_to + closureReason
 *  - SUSPENDED exige suspensionReason
 */
public final class PmmPlan {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");

    private static final Map<PmmPlanStatus, Set<PmmPlanStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(PmmPlanStatus.class);
        ALLOWED.put(PmmPlanStatus.DRAFT,
                EnumSet.of(PmmPlanStatus.ACTIVE, PmmPlanStatus.CLOSED));
        ALLOWED.put(PmmPlanStatus.ACTIVE,
                EnumSet.of(PmmPlanStatus.SUSPENDED, PmmPlanStatus.CLOSED));
        ALLOWED.put(PmmPlanStatus.SUSPENDED,
                EnumSet.of(PmmPlanStatus.ACTIVE, PmmPlanStatus.CLOSED));
        ALLOWED.put(PmmPlanStatus.CLOSED, EnumSet.noneOf(PmmPlanStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private final UUID aiSystemId;
    private String name;
    private String description;
    private String metricsMonitored;
    private String collectionMethod;
    private PmmReviewFrequency reviewFrequency;
    private String responsiblePartyDescription;
    private String triggerCriteria;
    private String qmsLinkReference;
    private PmmPlanStatus status;
    private Instant activatedAt;
    private Instant lastReviewedAt;
    private UUID lastReviewedByUserId;
    private Instant suspendedAt;
    private String suspensionReason;
    private Instant effectiveTo;
    private String closureReason;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public PmmPlan(UUID id, UUID tenantId, String reference, UUID aiSystemId,
                   String name, String description,
                   String metricsMonitored, String collectionMethod,
                   PmmReviewFrequency reviewFrequency,
                   String responsiblePartyDescription, String triggerCriteria,
                   String qmsLinkReference,
                   PmmPlanStatus status,
                   Instant activatedAt,
                   Instant lastReviewedAt, UUID lastReviewedByUserId,
                   Instant suspendedAt, String suspensionReason,
                   Instant effectiveTo, String closureReason,
                   UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.aiSystemId = Objects.requireNonNull(aiSystemId, "aiSystemId");
        this.name = requireText(name, "name", 250);
        this.description = description;
        this.metricsMonitored = metricsMonitored;
        this.collectionMethod = collectionMethod;
        this.reviewFrequency = reviewFrequency;
        this.responsiblePartyDescription = responsiblePartyDescription;
        this.triggerCriteria = triggerCriteria;
        this.qmsLinkReference = qmsLinkReference;
        this.status = Objects.requireNonNull(status, "status");
        this.activatedAt = activatedAt;
        this.lastReviewedAt = lastReviewedAt;
        this.lastReviewedByUserId = lastReviewedByUserId;
        this.suspendedAt = suspendedAt;
        this.suspensionReason = suspensionReason;
        this.effectiveTo = effectiveTo;
        this.closureReason = closureReason;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    public static PmmPlan draft(UUID tenantId, String reference, UUID aiSystemId,
                                String name, String description,
                                String metricsMonitored, String collectionMethod,
                                PmmReviewFrequency reviewFrequency,
                                String responsiblePartyDescription, String triggerCriteria,
                                String qmsLinkReference,
                                UUID createdByUserId, Instant now) {
        return new PmmPlan(null, tenantId, reference, aiSystemId, name, description,
                metricsMonitored, collectionMethod, reviewFrequency,
                responsiblePartyDescription, triggerCriteria, qmsLinkReference,
                PmmPlanStatus.DRAFT,
                null, null, null, null, null, null, null,
                createdByUserId, now, now);
    }

    public void editDraft(String name, String description,
                          String metricsMonitored, String collectionMethod,
                          PmmReviewFrequency reviewFrequency,
                          String responsiblePartyDescription, String triggerCriteria,
                          String qmsLinkReference, Instant now) {
        if (status != PmmPlanStatus.DRAFT) {
            throw new PmmPlanStateException("Only DRAFT plans can be edited");
        }
        this.name = requireText(name, "name", 250);
        this.description = description;
        this.metricsMonitored = metricsMonitored;
        this.collectionMethod = collectionMethod;
        this.reviewFrequency = reviewFrequency;
        this.responsiblePartyDescription = responsiblePartyDescription;
        this.triggerCriteria = triggerCriteria;
        this.qmsLinkReference = qmsLinkReference;
        this.updatedAt = now;
    }

    public void activate(Instant now) {
        ensureTransition(PmmPlanStatus.ACTIVE);
        if (metricsMonitored == null || metricsMonitored.isBlank()) {
            throw new PmmPlanStateException("metricsMonitored required to activate");
        }
        if (collectionMethod == null || collectionMethod.isBlank()) {
            throw new PmmPlanStateException("collectionMethod required to activate");
        }
        if (reviewFrequency == null) {
            throw new PmmPlanStateException("reviewFrequency required to activate");
        }
        if (status == PmmPlanStatus.DRAFT) {
            this.activatedAt = now;
        } else {
            this.suspendedAt = null;
            this.suspensionReason = null;
        }
        this.status = PmmPlanStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void recordReview(UUID reviewerId, Instant now) {
        if (status != PmmPlanStatus.ACTIVE) {
            throw new PmmPlanStateException("Reviews are only allowed when plan is ACTIVE");
        }
        Objects.requireNonNull(reviewerId, "reviewerId");
        this.lastReviewedAt = now;
        this.lastReviewedByUserId = reviewerId;
        this.updatedAt = now;
    }

    public void suspend(String reason, Instant now) {
        ensureTransition(PmmPlanStatus.SUSPENDED);
        if (reason == null || reason.isBlank()) {
            throw new PmmPlanStateException("suspension reason required");
        }
        this.status = PmmPlanStatus.SUSPENDED;
        this.suspendedAt = now;
        this.suspensionReason = reason;
        this.updatedAt = now;
    }

    public void close(String reason, Instant now) {
        ensureTransition(PmmPlanStatus.CLOSED);
        if (reason == null || reason.isBlank()) {
            throw new PmmPlanStateException("closure reason required");
        }
        this.status = PmmPlanStatus.CLOSED;
        this.effectiveTo = now;
        this.closureReason = reason;
        this.updatedAt = now;
    }

    public boolean isDraft()     { return status == PmmPlanStatus.DRAFT; }
    public boolean isActive()    { return status == PmmPlanStatus.ACTIVE; }
    public boolean isSuspended() { return status == PmmPlanStatus.SUSPENDED; }
    public boolean isClosed()    { return status == PmmPlanStatus.CLOSED; }

    public Instant nextReviewDueAt() {
        if (status != PmmPlanStatus.ACTIVE || reviewFrequency == null) return null;
        Instant base = lastReviewedAt != null ? lastReviewedAt : activatedAt;
        return base != null ? base.plus(reviewFrequency.period()) : null;
    }

    public boolean isReviewOverdue(Instant now) {
        Instant due = nextReviewDueAt();
        return due != null && now.isAfter(due);
    }

    private void ensureTransition(PmmPlanStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new PmmPlanStateException(
                    "Transition " + status + " → " + target + " is not allowed");
        }
    }

    private static String requireReference(String v) {
        if (v == null || !REF_PATTERN.matcher(v).matches()) {
            throw new IllegalArgumentException(
                    "reference must match [A-Z][A-Z0-9_-]{1,63}");
        }
        return v;
    }
    private static String requireText(String v, String f, int maxLen) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(f + " required");
        if (v.length() > maxLen) throw new IllegalArgumentException(
                f + " too long (max " + maxLen + ")");
        return v;
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getReference() { return reference; }
    public UUID getAiSystemId() { return aiSystemId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getMetricsMonitored() { return metricsMonitored; }
    public String getCollectionMethod() { return collectionMethod; }
    public PmmReviewFrequency getReviewFrequency() { return reviewFrequency; }
    public String getResponsiblePartyDescription() { return responsiblePartyDescription; }
    public String getTriggerCriteria() { return triggerCriteria; }
    public String getQmsLinkReference() { return qmsLinkReference; }
    public PmmPlanStatus getStatus() { return status; }
    public Instant getActivatedAt() { return activatedAt; }
    public Instant getLastReviewedAt() { return lastReviewedAt; }
    public UUID getLastReviewedByUserId() { return lastReviewedByUserId; }
    public Instant getSuspendedAt() { return suspendedAt; }
    public String getSuspensionReason() { return suspensionReason; }
    public Instant getEffectiveTo() { return effectiveTo; }
    public String getClosureReason() { return closureReason; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
