package com.openlab.qualitos.quality.aiconformity.domain;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat — évaluation de conformité d'un système IA (AI Act Art. 43).
 *
 * Procédures :
 *  - INTERNAL_CONTROL (Annexe VI) : auto-évaluation provider.
 *  - NOTIFIED_BODY (Annexe VII) : obligatoire pour certains HIGH-risk.
 *
 * Cycle de vie :
 *   PLANNED → IN_PROGRESS → CERTIFIED → EXPIRED
 *   PLANNED|IN_PROGRESS|CERTIFIED → REVOKED
 *   PLANNED|IN_PROGRESS → FAILED
 *
 * Garde-fous (dupliqués DB) :
 *  - NOTIFIED_BODY exige notifiedBodyId et notifiedBodyName.
 *  - CERTIFIED exige certificateNumber + certifiedAt + validUntil + EU declaration.
 *  - EXPIRED exige status précédent CERTIFIED et expiredAt ≥ validUntil.
 *  - REVOKED exige revocationReason + revokedAt.
 *  - FAILED exige failureReason.
 *  - validUntil > certifiedAt.
 */
public final class ConformityAssessment {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");
    /** Notified body identifier : 4-chiffres assigné par la Commission UE. */
    private static final Pattern NB_ID_PATTERN = Pattern.compile("^[0-9]{4}$");

    private static final Map<ConformityAssessmentStatus, Set<ConformityAssessmentStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(ConformityAssessmentStatus.class);
        ALLOWED.put(ConformityAssessmentStatus.PLANNED,
                EnumSet.of(ConformityAssessmentStatus.IN_PROGRESS,
                           ConformityAssessmentStatus.REVOKED,
                           ConformityAssessmentStatus.FAILED));
        ALLOWED.put(ConformityAssessmentStatus.IN_PROGRESS,
                EnumSet.of(ConformityAssessmentStatus.CERTIFIED,
                           ConformityAssessmentStatus.REVOKED,
                           ConformityAssessmentStatus.FAILED));
        ALLOWED.put(ConformityAssessmentStatus.CERTIFIED,
                EnumSet.of(ConformityAssessmentStatus.EXPIRED,
                           ConformityAssessmentStatus.REVOKED));
        ALLOWED.put(ConformityAssessmentStatus.EXPIRED,
                EnumSet.noneOf(ConformityAssessmentStatus.class));
        ALLOWED.put(ConformityAssessmentStatus.REVOKED,
                EnumSet.noneOf(ConformityAssessmentStatus.class));
        ALLOWED.put(ConformityAssessmentStatus.FAILED,
                EnumSet.noneOf(ConformityAssessmentStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private final UUID aiSystemId;
    private UUID qmsId;
    private final ConformityProcedure procedure;
    private String notifiedBodyId;
    private String notifiedBodyName;
    private String scope;
    private ConformityAssessmentStatus status;
    private Instant plannedAt;
    private Instant startedAt;
    private Instant certifiedAt;
    private String certificateNumber;
    private Instant validUntil;
    private String euDeclarationReference;
    private Instant expiredAt;
    private Instant revokedAt;
    private String revocationReason;
    private Instant failedAt;
    private String failureReason;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public ConformityAssessment(UUID id, UUID tenantId, String reference, UUID aiSystemId,
                                UUID qmsId, ConformityProcedure procedure,
                                String notifiedBodyId, String notifiedBodyName,
                                String scope,
                                ConformityAssessmentStatus status,
                                Instant plannedAt, Instant startedAt,
                                Instant certifiedAt, String certificateNumber,
                                Instant validUntil, String euDeclarationReference,
                                Instant expiredAt,
                                Instant revokedAt, String revocationReason,
                                Instant failedAt, String failureReason,
                                UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.aiSystemId = Objects.requireNonNull(aiSystemId, "aiSystemId");
        this.qmsId = qmsId;
        this.procedure = Objects.requireNonNull(procedure, "procedure");
        this.notifiedBodyId = sanitizeNbId(notifiedBodyId);
        this.notifiedBodyName = notifiedBodyName;
        this.scope = requireText(scope, "scope", 4000);
        this.status = Objects.requireNonNull(status, "status");
        this.plannedAt = plannedAt;
        this.startedAt = startedAt;
        this.certifiedAt = certifiedAt;
        this.certificateNumber = certificateNumber;
        this.validUntil = validUntil;
        this.euDeclarationReference = euDeclarationReference;
        this.expiredAt = expiredAt;
        this.revokedAt = revokedAt;
        this.revocationReason = revocationReason;
        this.failedAt = failedAt;
        this.failureReason = failureReason;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    public static ConformityAssessment plan(UUID tenantId, String reference, UUID aiSystemId,
                                            UUID qmsId, ConformityProcedure procedure,
                                            String notifiedBodyId, String notifiedBodyName,
                                            String scope,
                                            UUID createdByUserId, Instant now) {
        if (procedure == ConformityProcedure.NOTIFIED_BODY) {
            if (notifiedBodyId == null || notifiedBodyId.isBlank()
                    || notifiedBodyName == null || notifiedBodyName.isBlank()) {
                throw new ConformityAssessmentStateException(
                        "NOTIFIED_BODY procedure requires notifiedBodyId and notifiedBodyName");
            }
        }
        return new ConformityAssessment(null, tenantId, reference, aiSystemId, qmsId, procedure,
                notifiedBodyId, notifiedBodyName, scope,
                ConformityAssessmentStatus.PLANNED, now,
                null, null, null, null, null, null, null, null, null, null,
                createdByUserId, now, now);
    }

    public void editPlanned(UUID qmsId,
                            String notifiedBodyId, String notifiedBodyName,
                            String scope, Instant now) {
        if (status != ConformityAssessmentStatus.PLANNED) {
            throw new ConformityAssessmentStateException("Only PLANNED assessments can be edited");
        }
        if (procedure == ConformityProcedure.NOTIFIED_BODY) {
            if (notifiedBodyId == null || notifiedBodyId.isBlank()
                    || notifiedBodyName == null || notifiedBodyName.isBlank()) {
                throw new ConformityAssessmentStateException(
                        "NOTIFIED_BODY procedure requires notifiedBodyId and notifiedBodyName");
            }
        }
        this.qmsId = qmsId;
        this.notifiedBodyId = sanitizeNbId(notifiedBodyId);
        this.notifiedBodyName = notifiedBodyName;
        this.scope = requireText(scope, "scope", 4000);
        this.updatedAt = now;
    }

    public void start(Instant now) {
        ensureTransition(ConformityAssessmentStatus.IN_PROGRESS);
        this.status = ConformityAssessmentStatus.IN_PROGRESS;
        this.startedAt = now;
        this.updatedAt = now;
    }

    public void certify(String certificateNumber, String euDeclarationReference,
                        Instant validUntil, Instant now) {
        ensureTransition(ConformityAssessmentStatus.CERTIFIED);
        if (certificateNumber == null || certificateNumber.isBlank()) {
            throw new ConformityAssessmentStateException("certificateNumber required");
        }
        if (euDeclarationReference == null || euDeclarationReference.isBlank()) {
            throw new ConformityAssessmentStateException("euDeclarationReference required");
        }
        if (validUntil == null) {
            throw new ConformityAssessmentStateException("validUntil required");
        }
        if (!validUntil.isAfter(now)) {
            throw new ConformityAssessmentStateException("validUntil must be after certifiedAt");
        }
        this.status = ConformityAssessmentStatus.CERTIFIED;
        this.certifiedAt = now;
        this.certificateNumber = certificateNumber;
        this.euDeclarationReference = euDeclarationReference;
        this.validUntil = validUntil;
        this.updatedAt = now;
    }

    public void markExpired(Instant now) {
        ensureTransition(ConformityAssessmentStatus.EXPIRED);
        if (validUntil != null && now.isBefore(validUntil)) {
            throw new ConformityAssessmentStateException(
                    "Cannot mark expired before validUntil");
        }
        this.status = ConformityAssessmentStatus.EXPIRED;
        this.expiredAt = now;
        this.updatedAt = now;
    }

    public void revoke(String reason, Instant now) {
        ensureTransition(ConformityAssessmentStatus.REVOKED);
        if (reason == null || reason.isBlank()) {
            throw new ConformityAssessmentStateException("revocation reason required");
        }
        this.status = ConformityAssessmentStatus.REVOKED;
        this.revokedAt = now;
        this.revocationReason = reason;
        this.updatedAt = now;
    }

    public void markFailed(String reason, Instant now) {
        ensureTransition(ConformityAssessmentStatus.FAILED);
        if (reason == null || reason.isBlank()) {
            throw new ConformityAssessmentStateException("failure reason required");
        }
        this.status = ConformityAssessmentStatus.FAILED;
        this.failedAt = now;
        this.failureReason = reason;
        this.updatedAt = now;
    }

    public boolean isPlanned()      { return status == ConformityAssessmentStatus.PLANNED; }
    public boolean isCertified()    { return status == ConformityAssessmentStatus.CERTIFIED; }
    public boolean isInProgress()   { return status == ConformityAssessmentStatus.IN_PROGRESS; }
    public boolean isTerminal()     {
        return status == ConformityAssessmentStatus.EXPIRED
                || status == ConformityAssessmentStatus.REVOKED
                || status == ConformityAssessmentStatus.FAILED;
    }

    public boolean isCertificateExpired(Instant now) {
        return isCertified() && validUntil != null && now.isAfter(validUntil);
    }

    private void ensureTransition(ConformityAssessmentStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new ConformityAssessmentStateException(
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
    private static String sanitizeNbId(String v) {
        if (v == null) return null;
        String t = v.trim();
        if (t.isEmpty()) return null;
        if (!NB_ID_PATTERN.matcher(t).matches()) {
            throw new IllegalArgumentException(
                    "notifiedBodyId must be a 4-digit EU notified body number");
        }
        return t;
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
    public UUID getQmsId() { return qmsId; }
    public ConformityProcedure getProcedure() { return procedure; }
    public String getNotifiedBodyId() { return notifiedBodyId; }
    public String getNotifiedBodyName() { return notifiedBodyName; }
    public String getScope() { return scope; }
    public ConformityAssessmentStatus getStatus() { return status; }
    public Instant getPlannedAt() { return plannedAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCertifiedAt() { return certifiedAt; }
    public String getCertificateNumber() { return certificateNumber; }
    public Instant getValidUntil() { return validUntil; }
    public String getEuDeclarationReference() { return euDeclarationReference; }
    public Instant getExpiredAt() { return expiredAt; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getRevocationReason() { return revocationReason; }
    public Instant getFailedAt() { return failedAt; }
    public String getFailureReason() { return failureReason; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
