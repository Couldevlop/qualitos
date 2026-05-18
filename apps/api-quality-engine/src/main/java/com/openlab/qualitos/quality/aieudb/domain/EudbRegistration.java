package com.openlab.qualitos.quality.aieudb.domain;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat — enregistrement EUDB d'un système IA HIGH-risk (AI Act Art. 49 / 71).
 *
 * Avant la mise sur le marché, le provider doit enregistrer le système dans
 * la base européenne des systèmes IA HIGH-risk. Toute mise à jour significative
 * (architecture, finalité, données d'entraînement) entraîne une re-déclaration.
 *
 * Cycle de vie :
 *   DRAFT → SUBMITTED → REGISTERED (UPDATED ↔ UPDATED, REGISTERED)
 *   DRAFT|SUBMITTED → REJECTED
 *   REGISTERED|UPDATED → RETIRED
 *
 * Garde-fous (dupliqués DB) :
 *  - submission exige providerEntityName + memberStateOfReference + intendedPurposeSummary.
 *  - REGISTERED/UPDATED exigent eudbId + registrationDate.
 *  - UPDATED exige lastUpdateDate ≥ registrationDate.
 *  - REJECTED exige rejectionReason.
 *  - RETIRED exige retirementReason + retiredAt.
 */
public final class EudbRegistration {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");
    /** EUDB id format : EUDB-AI-XXXXXX (préfixe officiel + 6+ alphanumérique). */
    private static final Pattern EUDB_ID_PATTERN = Pattern.compile("^EUDB-AI-[A-Z0-9]{6,32}$");
    /** Code pays ISO 3166-1 alpha-2 (UE/EEE). */
    private static final Pattern MS_PATTERN = Pattern.compile("^[A-Z]{2}$");

    private static final Map<EudbRegistrationStatus, Set<EudbRegistrationStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(EudbRegistrationStatus.class);
        ALLOWED.put(EudbRegistrationStatus.DRAFT,
                EnumSet.of(EudbRegistrationStatus.SUBMITTED, EudbRegistrationStatus.REJECTED));
        ALLOWED.put(EudbRegistrationStatus.SUBMITTED,
                EnumSet.of(EudbRegistrationStatus.REGISTERED, EudbRegistrationStatus.REJECTED));
        ALLOWED.put(EudbRegistrationStatus.REGISTERED,
                EnumSet.of(EudbRegistrationStatus.UPDATED, EudbRegistrationStatus.RETIRED));
        ALLOWED.put(EudbRegistrationStatus.UPDATED,
                EnumSet.of(EudbRegistrationStatus.UPDATED, EudbRegistrationStatus.RETIRED));
        ALLOWED.put(EudbRegistrationStatus.REJECTED,  EnumSet.noneOf(EudbRegistrationStatus.class));
        ALLOWED.put(EudbRegistrationStatus.RETIRED,   EnumSet.noneOf(EudbRegistrationStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private final UUID aiSystemId;
    private String providerEntityName;
    private String providerEuRepresentative;
    private String memberStateOfReference;
    private String intendedPurposeSummary;
    private String technicalDocumentationReference;
    private String eudbId;
    private EudbRegistrationStatus status;
    private Instant submittedAt;
    private UUID submittedByUserId;
    private Instant registrationDate;
    private Instant lastUpdateDate;
    private String lastUpdateSummary;
    private Instant rejectedAt;
    private String rejectionReason;
    private Instant retiredAt;
    private String retirementReason;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public EudbRegistration(UUID id, UUID tenantId, String reference, UUID aiSystemId,
                            String providerEntityName, String providerEuRepresentative,
                            String memberStateOfReference, String intendedPurposeSummary,
                            String technicalDocumentationReference,
                            String eudbId,
                            EudbRegistrationStatus status,
                            Instant submittedAt, UUID submittedByUserId,
                            Instant registrationDate,
                            Instant lastUpdateDate, String lastUpdateSummary,
                            Instant rejectedAt, String rejectionReason,
                            Instant retiredAt, String retirementReason,
                            UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.aiSystemId = Objects.requireNonNull(aiSystemId, "aiSystemId");
        this.providerEntityName = providerEntityName;
        this.providerEuRepresentative = providerEuRepresentative;
        this.memberStateOfReference = sanitizeMemberState(memberStateOfReference);
        this.intendedPurposeSummary = intendedPurposeSummary;
        this.technicalDocumentationReference = technicalDocumentationReference;
        this.eudbId = sanitizeEudbId(eudbId);
        this.status = Objects.requireNonNull(status, "status");
        this.submittedAt = submittedAt;
        this.submittedByUserId = submittedByUserId;
        this.registrationDate = registrationDate;
        this.lastUpdateDate = lastUpdateDate;
        this.lastUpdateSummary = lastUpdateSummary;
        this.rejectedAt = rejectedAt;
        this.rejectionReason = rejectionReason;
        this.retiredAt = retiredAt;
        this.retirementReason = retirementReason;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    public static EudbRegistration draft(UUID tenantId, String reference, UUID aiSystemId,
                                         String providerEntityName,
                                         String providerEuRepresentative,
                                         String memberStateOfReference,
                                         String intendedPurposeSummary,
                                         String technicalDocumentationReference,
                                         UUID createdByUserId, Instant now) {
        return new EudbRegistration(null, tenantId, reference, aiSystemId,
                providerEntityName, providerEuRepresentative, memberStateOfReference,
                intendedPurposeSummary, technicalDocumentationReference, null,
                EudbRegistrationStatus.DRAFT,
                null, null, null, null, null, null, null, null, null,
                createdByUserId, now, now);
    }

    public void editDraft(String providerEntityName, String providerEuRepresentative,
                          String memberStateOfReference, String intendedPurposeSummary,
                          String technicalDocumentationReference, Instant now) {
        if (status != EudbRegistrationStatus.DRAFT) {
            throw new EudbRegistrationStateException("Only DRAFT registrations can be edited");
        }
        this.providerEntityName = providerEntityName;
        this.providerEuRepresentative = providerEuRepresentative;
        this.memberStateOfReference = sanitizeMemberState(memberStateOfReference);
        this.intendedPurposeSummary = intendedPurposeSummary;
        this.technicalDocumentationReference = technicalDocumentationReference;
        this.updatedAt = now;
    }

    public void submit(UUID submitterId, Instant now) {
        ensureTransition(EudbRegistrationStatus.SUBMITTED);
        Objects.requireNonNull(submitterId, "submitterId");
        requireNonBlank(providerEntityName, "providerEntityName");
        if (memberStateOfReference == null) {
            throw new EudbRegistrationStateException("memberStateOfReference required for submission");
        }
        requireNonBlank(intendedPurposeSummary, "intendedPurposeSummary");
        this.status = EudbRegistrationStatus.SUBMITTED;
        this.submittedAt = now;
        this.submittedByUserId = submitterId;
        this.updatedAt = now;
    }

    public void markRegistered(String eudbId, Instant registrationDate, Instant now) {
        ensureTransition(EudbRegistrationStatus.REGISTERED);
        this.eudbId = sanitizeEudbId(eudbId);
        if (this.eudbId == null) {
            throw new EudbRegistrationStateException("eudbId required for registration");
        }
        if (registrationDate == null) {
            throw new EudbRegistrationStateException("registrationDate required");
        }
        this.status = EudbRegistrationStatus.REGISTERED;
        this.registrationDate = registrationDate;
        this.updatedAt = now;
    }

    public void declareUpdate(String updateSummary, Instant updateDate, Instant now) {
        ensureTransition(EudbRegistrationStatus.UPDATED);
        if (updateSummary == null || updateSummary.isBlank()) {
            throw new EudbRegistrationStateException("update summary required");
        }
        if (updateDate == null) {
            throw new EudbRegistrationStateException("updateDate required");
        }
        if (registrationDate != null && updateDate.isBefore(registrationDate)) {
            throw new EudbRegistrationStateException(
                    "updateDate must be ≥ registrationDate");
        }
        if (lastUpdateDate != null && updateDate.isBefore(lastUpdateDate)) {
            throw new EudbRegistrationStateException(
                    "updateDate must be ≥ previous lastUpdateDate");
        }
        this.status = EudbRegistrationStatus.UPDATED;
        this.lastUpdateDate = updateDate;
        this.lastUpdateSummary = updateSummary;
        this.updatedAt = now;
    }

    public void reject(String reason, Instant now) {
        ensureTransition(EudbRegistrationStatus.REJECTED);
        if (reason == null || reason.isBlank()) {
            throw new EudbRegistrationStateException("rejection reason required");
        }
        this.status = EudbRegistrationStatus.REJECTED;
        this.rejectedAt = now;
        this.rejectionReason = reason;
        this.updatedAt = now;
    }

    public void retire(String reason, Instant now) {
        ensureTransition(EudbRegistrationStatus.RETIRED);
        if (reason == null || reason.isBlank()) {
            throw new EudbRegistrationStateException("retirement reason required");
        }
        this.status = EudbRegistrationStatus.RETIRED;
        this.retiredAt = now;
        this.retirementReason = reason;
        this.updatedAt = now;
    }

    public boolean isDraft()      { return status == EudbRegistrationStatus.DRAFT; }
    public boolean isRegistered() { return status == EudbRegistrationStatus.REGISTERED; }
    public boolean isUpdated()    { return status == EudbRegistrationStatus.UPDATED; }
    public boolean isActive()     { return isRegistered() || isUpdated(); }

    private static void requireNonBlank(String v, String f) {
        if (v == null || v.isBlank()) {
            throw new EudbRegistrationStateException(f + " required for submission");
        }
    }

    private void ensureTransition(EudbRegistrationStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new EudbRegistrationStateException(
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
    private static String sanitizeEudbId(String v) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        if (!EUDB_ID_PATTERN.matcher(t).matches()) {
            throw new IllegalArgumentException(
                    "eudbId must match EUDB-AI-[A-Z0-9]{6,32}");
        }
        return t;
    }
    private static String sanitizeMemberState(String v) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        if (!MS_PATTERN.matcher(t).matches()) {
            throw new IllegalArgumentException(
                    "memberStateOfReference must be ISO-3166-1 alpha-2 (e.g., FR)");
        }
        return t;
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getReference() { return reference; }
    public UUID getAiSystemId() { return aiSystemId; }
    public String getProviderEntityName() { return providerEntityName; }
    public String getProviderEuRepresentative() { return providerEuRepresentative; }
    public String getMemberStateOfReference() { return memberStateOfReference; }
    public String getIntendedPurposeSummary() { return intendedPurposeSummary; }
    public String getTechnicalDocumentationReference() { return technicalDocumentationReference; }
    public String getEudbId() { return eudbId; }
    public EudbRegistrationStatus getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public UUID getSubmittedByUserId() { return submittedByUserId; }
    public Instant getRegistrationDate() { return registrationDate; }
    public Instant getLastUpdateDate() { return lastUpdateDate; }
    public String getLastUpdateSummary() { return lastUpdateSummary; }
    public Instant getRejectedAt() { return rejectedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public Instant getRetiredAt() { return retiredAt; }
    public String getRetirementReason() { return retirementReason; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
