package com.openlab.qualitos.quality.cyberincidents.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat — incident de cybersécurité au sens NIS2 (directive (UE) 2022/2555).
 *
 * Trois délais Art. 23 NIS2 :
 *  - 24h : alerte préliminaire au CSIRT (Art. 23.4.a)
 *  - 72h : évaluation initiale (Art. 23.4.b)
 *  - 30 jours : rapport final (Art. 23.4.c)
 *
 * Lien optionnel vers un incident RGPD si l'incident cyber a entraîné une
 * violation de données personnelles (un même évènement peut être à la fois
 * "data breach" RGPD et "incident significatif" NIS2 — les deux régimes
 * coexistent et imposent leurs propres notifications).
 *
 * Cycle de vie :
 *   DETECTED → ASSESSING → MITIGATED → CLOSED
 *   DETECTED|ASSESSING → REJECTED
 *
 * Privacy by design (OWASP A02) : l'agrégat décrit l'incident technique,
 * pas les personnes affectées (référencées via {@code linkedBreachId} si
 * applicable). {@code affectedAssets} et {@code affectedServices} sont des
 * codes structurels, pas des identifiants nominatifs.
 */
public final class CyberIncident {

    public static final Duration EARLY_WARNING_WINDOW   = Duration.ofHours(24);
    public static final Duration INITIAL_ASSESSMENT_WIN = Duration.ofHours(72);
    public static final Duration FINAL_REPORT_WINDOW    = Duration.ofDays(30);

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");
    private static final Pattern CODE_PATTERN = Pattern.compile("^[a-z][a-z0-9._-]{1,63}$");

    private static final Map<CyberIncidentStatus, Set<CyberIncidentStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(CyberIncidentStatus.class);
        ALLOWED.put(CyberIncidentStatus.DETECTED,
                EnumSet.of(CyberIncidentStatus.ASSESSING, CyberIncidentStatus.REJECTED));
        ALLOWED.put(CyberIncidentStatus.ASSESSING,
                EnumSet.of(CyberIncidentStatus.MITIGATED, CyberIncidentStatus.REJECTED));
        ALLOWED.put(CyberIncidentStatus.MITIGATED,
                EnumSet.of(CyberIncidentStatus.CLOSED));
        ALLOWED.put(CyberIncidentStatus.CLOSED,
                EnumSet.noneOf(CyberIncidentStatus.class));
        ALLOWED.put(CyberIncidentStatus.REJECTED,
                EnumSet.noneOf(CyberIncidentStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private String title;
    private String description;
    private final Instant detectedAt;
    private final Instant occurredAt;
    private final Instant earlyWarningDeadlineAt;
    private final Instant initialAssessmentDeadlineAt;
    private final Instant finalReportDeadlineAt;
    private CyberIncidentType incidentType;
    private CyberIncidentSeverity severity;
    private CyberIncidentStatus status;
    private long estimatedAffectedUsers;
    private final Set<String> affectedAssets;
    private final Set<String> affectedServices;
    private UUID linkedBreachId;
    private String containmentMeasures;
    private String impactDescription;
    private Instant earlyWarningSentAt;
    private String earlyWarningReference;
    private Instant initialAssessmentSentAt;
    private String initialAssessmentReference;
    private Instant finalReportSentAt;
    private String finalReportReference;
    private String closureNotes;
    private String rejectionReason;
    private final UUID reportedByUserId;
    private UUID handledByUserId;
    private Instant closedAt;
    private Instant updatedAt;

    public CyberIncident(UUID id, UUID tenantId, String reference,
                         String title, String description,
                         Instant detectedAt, Instant occurredAt,
                         Instant earlyWarningDeadlineAt,
                         Instant initialAssessmentDeadlineAt,
                         Instant finalReportDeadlineAt,
                         CyberIncidentType incidentType, CyberIncidentSeverity severity,
                         CyberIncidentStatus status,
                         long estimatedAffectedUsers,
                         Set<String> affectedAssets, Set<String> affectedServices,
                         UUID linkedBreachId,
                         String containmentMeasures, String impactDescription,
                         Instant earlyWarningSentAt, String earlyWarningReference,
                         Instant initialAssessmentSentAt, String initialAssessmentReference,
                         Instant finalReportSentAt, String finalReportReference,
                         String closureNotes, String rejectionReason,
                         UUID reportedByUserId, UUID handledByUserId,
                         Instant closedAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.title = requireText(title, "title", 250);
        this.description = description;
        this.detectedAt = Objects.requireNonNull(detectedAt, "detectedAt");
        this.occurredAt = occurredAt;
        this.earlyWarningDeadlineAt = Objects.requireNonNull(earlyWarningDeadlineAt, "earlyWarningDeadlineAt");
        this.initialAssessmentDeadlineAt = Objects.requireNonNull(initialAssessmentDeadlineAt, "initialAssessmentDeadlineAt");
        this.finalReportDeadlineAt = Objects.requireNonNull(finalReportDeadlineAt, "finalReportDeadlineAt");
        this.incidentType = Objects.requireNonNull(incidentType, "incidentType");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.status = Objects.requireNonNull(status, "status");
        this.estimatedAffectedUsers = requireNonNegative(estimatedAffectedUsers);
        this.affectedAssets = sanitizeCodes(affectedAssets, "affectedAssets");
        this.affectedServices = sanitizeCodes(affectedServices, "affectedServices");
        this.linkedBreachId = linkedBreachId;
        this.containmentMeasures = containmentMeasures;
        this.impactDescription = impactDescription;
        this.earlyWarningSentAt = earlyWarningSentAt;
        this.earlyWarningReference = earlyWarningReference;
        this.initialAssessmentSentAt = initialAssessmentSentAt;
        this.initialAssessmentReference = initialAssessmentReference;
        this.finalReportSentAt = finalReportSentAt;
        this.finalReportReference = finalReportReference;
        this.closureNotes = closureNotes;
        this.rejectionReason = rejectionReason;
        this.reportedByUserId = Objects.requireNonNull(reportedByUserId, "reportedByUserId");
        this.handledByUserId = handledByUserId;
        this.closedAt = closedAt;
        this.updatedAt = updatedAt != null ? updatedAt : detectedAt;
    }

    /** Factory — détection d'un nouvel incident. */
    public static CyberIncident detect(UUID tenantId, String reference,
                                       String title, String description,
                                       Instant detectedAt, Instant occurredAt,
                                       CyberIncidentType incidentType,
                                       CyberIncidentSeverity severity,
                                       long estimatedAffectedUsers,
                                       Set<String> affectedAssets, Set<String> affectedServices,
                                       UUID linkedBreachId,
                                       UUID reportedByUserId) {
        return new CyberIncident(null, tenantId, reference, title, description,
                detectedAt, occurredAt,
                detectedAt.plus(EARLY_WARNING_WINDOW),
                detectedAt.plus(INITIAL_ASSESSMENT_WIN),
                detectedAt.plus(FINAL_REPORT_WINDOW),
                incidentType, severity, CyberIncidentStatus.DETECTED,
                estimatedAffectedUsers, affectedAssets, affectedServices,
                linkedBreachId, null, null,
                null, null, null, null, null, null,
                null, null,
                reportedByUserId, null, null, detectedAt);
    }

    public void startAssessment(UUID handledByUserId, Instant now) {
        ensureTransition(CyberIncidentStatus.ASSESSING);
        this.handledByUserId = handledByUserId;
        this.status = CyberIncidentStatus.ASSESSING;
        this.updatedAt = now;
    }

    public void mitigate(String measures, String impact, UUID handledByUserId, Instant now) {
        ensureTransition(CyberIncidentStatus.MITIGATED);
        if (measures == null || measures.isBlank()) {
            throw new CyberIncidentStateException("containmentMeasures required");
        }
        this.containmentMeasures = measures;
        this.impactDescription = impact;
        if (handledByUserId != null) this.handledByUserId = handledByUserId;
        this.status = CyberIncidentStatus.MITIGATED;
        this.updatedAt = now;
    }

    public void recordEarlyWarning(Instant sentAt, String reference, Instant now) {
        if (status == CyberIncidentStatus.REJECTED || status == CyberIncidentStatus.CLOSED) {
            throw new CyberIncidentStateException(
                    "Cannot record notification on terminal incident");
        }
        requireNotification(sentAt, reference, "earlyWarning");
        this.earlyWarningSentAt = sentAt;
        this.earlyWarningReference = reference;
        this.updatedAt = now;
    }

    public void recordInitialAssessment(Instant sentAt, String reference, Instant now) {
        if (status == CyberIncidentStatus.REJECTED || status == CyberIncidentStatus.CLOSED) {
            throw new CyberIncidentStateException(
                    "Cannot record notification on terminal incident");
        }
        requireNotification(sentAt, reference, "initialAssessment");
        this.initialAssessmentSentAt = sentAt;
        this.initialAssessmentReference = reference;
        this.updatedAt = now;
    }

    public void recordFinalReport(Instant sentAt, String reference, Instant now) {
        if (status == CyberIncidentStatus.REJECTED) {
            throw new CyberIncidentStateException(
                    "Cannot record final report on rejected incident");
        }
        requireNotification(sentAt, reference, "finalReport");
        this.finalReportSentAt = sentAt;
        this.finalReportReference = reference;
        this.updatedAt = now;
    }

    /** Clôture. Garde-fou : incidents significatifs (HIGH/CRITICAL) ne peuvent
     *  être clôturés que si le rapport final a été envoyé OU si {@code closureNotes}
     *  documente l'exception (NIS2 Art. 23.4.c). */
    public void close(String closureNotes, Instant now) {
        ensureTransition(CyberIncidentStatus.CLOSED);
        if (severity.isSignificant() && finalReportSentAt == null
                && (closureNotes == null || closureNotes.isBlank())) {
            throw new CyberIncidentStateException(
                    "severity=" + severity + " requires final report sent or "
                    + "explicit closureNotes documenting the exemption (NIS2 Art. 23.4.c)");
        }
        this.closureNotes = closureNotes;
        this.status = CyberIncidentStatus.CLOSED;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public void reject(String reason, Instant now) {
        ensureTransition(CyberIncidentStatus.REJECTED);
        if (reason == null || reason.isBlank()) {
            throw new CyberIncidentStateException("rejection reason required");
        }
        this.rejectionReason = reason;
        this.status = CyberIncidentStatus.REJECTED;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public void updateSeverity(CyberIncidentSeverity newSeverity, Instant now) {
        if (isTerminal()) {
            throw new CyberIncidentStateException(
                    "Cannot update severity on terminal incident");
        }
        this.severity = Objects.requireNonNull(newSeverity, "severity");
        this.updatedAt = now;
    }

    public void linkBreach(UUID breachId, Instant now) {
        if (isTerminal()) {
            throw new CyberIncidentStateException("Cannot link breach on terminal incident");
        }
        this.linkedBreachId = breachId;
        this.updatedAt = now;
    }

    public boolean isTerminal() {
        return status == CyberIncidentStatus.CLOSED || status == CyberIncidentStatus.REJECTED;
    }

    public boolean isEarlyWarningOverdue(Instant ref) {
        return earlyWarningSentAt == null
                && status != CyberIncidentStatus.REJECTED
                && ref.isAfter(earlyWarningDeadlineAt);
    }

    public boolean isInitialAssessmentOverdue(Instant ref) {
        return initialAssessmentSentAt == null
                && status != CyberIncidentStatus.REJECTED
                && ref.isAfter(initialAssessmentDeadlineAt);
    }

    public boolean isFinalReportOverdue(Instant ref) {
        return finalReportSentAt == null
                && status != CyberIncidentStatus.REJECTED
                && ref.isAfter(finalReportDeadlineAt);
    }

    private void requireNotification(Instant sentAt, String reference, String field) {
        if (sentAt == null) {
            throw new CyberIncidentStateException(field + ".sentAt required");
        }
        if (reference == null || reference.isBlank()) {
            throw new CyberIncidentStateException(field + ".reference required");
        }
        if (sentAt.isBefore(detectedAt)) {
            throw new CyberIncidentStateException(field + ".sentAt cannot precede detectedAt");
        }
    }

    private void ensureTransition(CyberIncidentStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new CyberIncidentStateException(
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
    private static long requireNonNegative(long v) {
        if (v < 0) throw new IllegalArgumentException("estimatedAffectedUsers must be ≥ 0");
        return v;
    }
    private static Set<String> sanitizeCodes(Set<String> input, String field) {
        if (input == null) return Collections.emptySet();
        Set<String> out = new LinkedHashSet<>();
        for (String c : input) {
            if (c == null) continue;
            String t = c.trim();
            if (t.isEmpty()) continue;
            if (!CODE_PATTERN.matcher(t).matches()) {
                throw new IllegalArgumentException(field + ": '" + t
                        + "' must match [a-z][a-z0-9._-]{1,63}");
            }
            out.add(t);
        }
        return Collections.unmodifiableSet(out);
    }

    public void assignId(UUID id) { this.id = id; }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public String getReference() { return reference; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Instant getDetectedAt() { return detectedAt; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getEarlyWarningDeadlineAt() { return earlyWarningDeadlineAt; }
    public Instant getInitialAssessmentDeadlineAt() { return initialAssessmentDeadlineAt; }
    public Instant getFinalReportDeadlineAt() { return finalReportDeadlineAt; }
    public CyberIncidentType getIncidentType() { return incidentType; }
    public CyberIncidentSeverity getSeverity() { return severity; }
    public CyberIncidentStatus getStatus() { return status; }
    public long getEstimatedAffectedUsers() { return estimatedAffectedUsers; }
    public Set<String> getAffectedAssets() { return affectedAssets; }
    public Set<String> getAffectedServices() { return affectedServices; }
    public UUID getLinkedBreachId() { return linkedBreachId; }
    public String getContainmentMeasures() { return containmentMeasures; }
    public String getImpactDescription() { return impactDescription; }
    public Instant getEarlyWarningSentAt() { return earlyWarningSentAt; }
    public String getEarlyWarningReference() { return earlyWarningReference; }
    public Instant getInitialAssessmentSentAt() { return initialAssessmentSentAt; }
    public String getInitialAssessmentReference() { return initialAssessmentReference; }
    public Instant getFinalReportSentAt() { return finalReportSentAt; }
    public String getFinalReportReference() { return finalReportReference; }
    public String getClosureNotes() { return closureNotes; }
    public String getRejectionReason() { return rejectionReason; }
    public UUID getReportedByUserId() { return reportedByUserId; }
    public UUID getHandledByUserId() { return handledByUserId; }
    public Instant getClosedAt() { return closedAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
