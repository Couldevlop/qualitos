package com.openlab.qualitos.quality.aiincidents.domain;

import java.time.Instant;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Agrégat — signalement d'incident IA grave (AI Act Art. 73).
 *
 * Cycle de vie :
 *   DETECTED → INVESTIGATING → NOTIFIED_REGULATOR → CLOSED
 *   DETECTED|INVESTIGATING → DISMISSED
 *
 * Garde-fous (dupliqués DB) :
 *  - notification au régulateur exige reference, occurredAt et description.
 *  - CLOSED exige rootCauseAnalysis + correctiveActions.
 *  - DISMISSED exige dismissalReason.
 *  - occurredAt ≤ detectedAt.
 */
public final class AiIncident {

    private static final Pattern REF_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_-]{1,63}$");

    private static final Map<AiIncidentStatus, Set<AiIncidentStatus>> ALLOWED;
    static {
        ALLOWED = new EnumMap<>(AiIncidentStatus.class);
        ALLOWED.put(AiIncidentStatus.DETECTED,
                EnumSet.of(AiIncidentStatus.INVESTIGATING, AiIncidentStatus.DISMISSED));
        ALLOWED.put(AiIncidentStatus.INVESTIGATING,
                EnumSet.of(AiIncidentStatus.NOTIFIED_REGULATOR, AiIncidentStatus.DISMISSED));
        ALLOWED.put(AiIncidentStatus.NOTIFIED_REGULATOR,
                EnumSet.of(AiIncidentStatus.CLOSED));
        ALLOWED.put(AiIncidentStatus.CLOSED, EnumSet.noneOf(AiIncidentStatus.class));
        ALLOWED.put(AiIncidentStatus.DISMISSED, EnumSet.noneOf(AiIncidentStatus.class));
    }

    private UUID id;
    private final UUID tenantId;
    private final String reference;
    private final UUID aiSystemId;
    private final AiIncidentSeverity severity;
    private String description;
    private String affectedPersonsDescription;
    private String immediateActionsTaken;
    private final Instant occurredAt;
    private final Instant detectedAt;
    private AiIncidentStatus status;
    private Instant investigationStartedAt;
    private UUID investigationLeadUserId;
    private String rootCauseAnalysis;
    private String correctiveActions;
    private Instant notifiedRegulatorAt;
    private String regulatorReference;
    private Instant closedAt;
    private Instant dismissedAt;
    private String dismissalReason;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;

    public AiIncident(UUID id, UUID tenantId, String reference, UUID aiSystemId,
                      AiIncidentSeverity severity, String description,
                      String affectedPersonsDescription, String immediateActionsTaken,
                      Instant occurredAt, Instant detectedAt,
                      AiIncidentStatus status,
                      Instant investigationStartedAt, UUID investigationLeadUserId,
                      String rootCauseAnalysis, String correctiveActions,
                      Instant notifiedRegulatorAt, String regulatorReference,
                      Instant closedAt, Instant dismissedAt, String dismissalReason,
                      UUID createdByUserId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.reference = requireReference(reference);
        this.aiSystemId = Objects.requireNonNull(aiSystemId, "aiSystemId");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.description = requireText(description, "description", 4000);
        this.affectedPersonsDescription = affectedPersonsDescription;
        this.immediateActionsTaken = immediateActionsTaken;
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt");
        this.detectedAt = Objects.requireNonNull(detectedAt, "detectedAt");
        if (occurredAt.isAfter(detectedAt)) {
            throw new IllegalArgumentException("occurredAt must be ≤ detectedAt");
        }
        this.status = Objects.requireNonNull(status, "status");
        this.investigationStartedAt = investigationStartedAt;
        this.investigationLeadUserId = investigationLeadUserId;
        this.rootCauseAnalysis = rootCauseAnalysis;
        this.correctiveActions = correctiveActions;
        this.notifiedRegulatorAt = notifiedRegulatorAt;
        this.regulatorReference = regulatorReference;
        this.closedAt = closedAt;
        this.dismissedAt = dismissedAt;
        this.dismissalReason = dismissalReason;
        this.createdByUserId = Objects.requireNonNull(createdByUserId, "createdByUserId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = updatedAt != null ? updatedAt : createdAt;
    }

    public static AiIncident detect(UUID tenantId, String reference, UUID aiSystemId,
                                    AiIncidentSeverity severity, String description,
                                    String affectedPersonsDescription, String immediateActionsTaken,
                                    Instant occurredAt, Instant detectedAt,
                                    UUID createdByUserId, Instant now) {
        return new AiIncident(null, tenantId, reference, aiSystemId, severity, description,
                affectedPersonsDescription, immediateActionsTaken, occurredAt, detectedAt,
                AiIncidentStatus.DETECTED,
                null, null, null, null, null, null, null, null, null,
                createdByUserId, now, now);
    }

    public void editDetected(String description, String affectedPersonsDescription,
                             String immediateActionsTaken, Instant now) {
        if (status != AiIncidentStatus.DETECTED) {
            throw new AiIncidentStateException("Only DETECTED incidents can be edited");
        }
        this.description = requireText(description, "description", 4000);
        this.affectedPersonsDescription = affectedPersonsDescription;
        this.immediateActionsTaken = immediateActionsTaken;
        this.updatedAt = now;
    }

    public void startInvestigation(UUID leadUserId, Instant now) {
        ensureTransition(AiIncidentStatus.INVESTIGATING);
        Objects.requireNonNull(leadUserId, "investigationLeadUserId");
        this.status = AiIncidentStatus.INVESTIGATING;
        this.investigationStartedAt = now;
        this.investigationLeadUserId = leadUserId;
        this.updatedAt = now;
    }

    public void notifyRegulator(String regulatorReference, String rootCauseAnalysis,
                                String correctiveActions, Instant now) {
        ensureTransition(AiIncidentStatus.NOTIFIED_REGULATOR);
        if (regulatorReference == null || regulatorReference.isBlank()) {
            throw new AiIncidentStateException("regulatorReference required");
        }
        if (rootCauseAnalysis == null || rootCauseAnalysis.isBlank()) {
            throw new AiIncidentStateException("rootCauseAnalysis required for notification");
        }
        this.regulatorReference = regulatorReference;
        this.rootCauseAnalysis = rootCauseAnalysis;
        this.correctiveActions = correctiveActions;
        this.notifiedRegulatorAt = now;
        this.status = AiIncidentStatus.NOTIFIED_REGULATOR;
        this.updatedAt = now;
    }

    public void close(String correctiveActions, Instant now) {
        ensureTransition(AiIncidentStatus.CLOSED);
        if (correctiveActions == null || correctiveActions.isBlank()) {
            throw new AiIncidentStateException("correctiveActions required to close");
        }
        if (rootCauseAnalysis == null || rootCauseAnalysis.isBlank()) {
            throw new AiIncidentStateException("rootCauseAnalysis required to close");
        }
        this.correctiveActions = correctiveActions;
        this.status = AiIncidentStatus.CLOSED;
        this.closedAt = now;
        this.updatedAt = now;
    }

    public void dismiss(String reason, Instant now) {
        ensureTransition(AiIncidentStatus.DISMISSED);
        if (reason == null || reason.isBlank()) {
            throw new AiIncidentStateException("dismissal reason required");
        }
        this.dismissalReason = reason;
        this.status = AiIncidentStatus.DISMISSED;
        this.dismissedAt = now;
        this.updatedAt = now;
    }

    public boolean isDetected()           { return status == AiIncidentStatus.DETECTED; }
    public boolean isClosed()             { return status == AiIncidentStatus.CLOSED; }
    public boolean isDismissed()          { return status == AiIncidentStatus.DISMISSED; }
    public boolean isTerminal()           { return isClosed() || isDismissed(); }
    public boolean isNotifiedRegulator()  { return status == AiIncidentStatus.NOTIFIED_REGULATOR; }

    /** Échéance réglementaire : detectedAt + délai sévérité. */
    public Instant regulatorNotificationDueAt() {
        return detectedAt.plus(severity.regulatorNotificationDeadline());
    }

    public boolean isRegulatorNotificationOverdue(Instant now) {
        if (status != AiIncidentStatus.DETECTED && status != AiIncidentStatus.INVESTIGATING) {
            return false;
        }
        return now.isAfter(regulatorNotificationDueAt());
    }

    private void ensureTransition(AiIncidentStatus target) {
        if (!ALLOWED.getOrDefault(status, Set.of()).contains(target)) {
            throw new AiIncidentStateException(
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
    public AiIncidentSeverity getSeverity() { return severity; }
    public String getDescription() { return description; }
    public String getAffectedPersonsDescription() { return affectedPersonsDescription; }
    public String getImmediateActionsTaken() { return immediateActionsTaken; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getDetectedAt() { return detectedAt; }
    public AiIncidentStatus getStatus() { return status; }
    public Instant getInvestigationStartedAt() { return investigationStartedAt; }
    public UUID getInvestigationLeadUserId() { return investigationLeadUserId; }
    public String getRootCauseAnalysis() { return rootCauseAnalysis; }
    public String getCorrectiveActions() { return correctiveActions; }
    public Instant getNotifiedRegulatorAt() { return notifiedRegulatorAt; }
    public String getRegulatorReference() { return regulatorReference; }
    public Instant getClosedAt() { return closedAt; }
    public Instant getDismissedAt() { return dismissedAt; }
    public String getDismissalReason() { return dismissalReason; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
