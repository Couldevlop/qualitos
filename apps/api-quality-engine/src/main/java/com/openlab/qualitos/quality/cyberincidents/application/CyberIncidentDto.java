package com.openlab.qualitos.quality.cyberincidents.application;

import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncident;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentSeverity;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentStatus;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class CyberIncidentDto {

    private CyberIncidentDto() {}

    public record DetectRequest(
            String reference, String title, String description,
            Instant detectedAt, Instant occurredAt,
            CyberIncidentType incidentType, CyberIncidentSeverity severity,
            long estimatedAffectedUsers,
            Set<String> affectedAssets, Set<String> affectedServices,
            UUID linkedBreachId, UUID reportedByUserId) {}

    public record StartAssessmentRequest(UUID handledByUserId) {}
    public record MitigateRequest(String containmentMeasures, String impactDescription, UUID handledByUserId) {}
    public record NotificationRequest(Instant sentAt, String reference) {}
    public record CloseRequest(String closureNotes) {}
    public record RejectRequest(String reason) {}
    public record UpdateSeverityRequest(CyberIncidentSeverity severity) {}
    public record LinkBreachRequest(UUID breachId) {}

    public record View(
            UUID id, UUID tenantId, String reference, String title, String description,
            Instant detectedAt, Instant occurredAt,
            Instant earlyWarningDeadlineAt, Instant initialAssessmentDeadlineAt,
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
            Instant closedAt, Instant updatedAt,
            boolean earlyWarningOverdue, boolean initialAssessmentOverdue,
            boolean finalReportOverdue, boolean significant
    ) {
        public static View of(CyberIncident i, Instant now) {
            return new View(
                    i.getId(), i.getTenantId(), i.getReference(), i.getTitle(), i.getDescription(),
                    i.getDetectedAt(), i.getOccurredAt(),
                    i.getEarlyWarningDeadlineAt(), i.getInitialAssessmentDeadlineAt(),
                    i.getFinalReportDeadlineAt(),
                    i.getIncidentType(), i.getSeverity(), i.getStatus(),
                    i.getEstimatedAffectedUsers(),
                    i.getAffectedAssets(), i.getAffectedServices(),
                    i.getLinkedBreachId(),
                    i.getContainmentMeasures(), i.getImpactDescription(),
                    i.getEarlyWarningSentAt(), i.getEarlyWarningReference(),
                    i.getInitialAssessmentSentAt(), i.getInitialAssessmentReference(),
                    i.getFinalReportSentAt(), i.getFinalReportReference(),
                    i.getClosureNotes(), i.getRejectionReason(),
                    i.getReportedByUserId(), i.getHandledByUserId(),
                    i.getClosedAt(), i.getUpdatedAt(),
                    i.isEarlyWarningOverdue(now),
                    i.isInitialAssessmentOverdue(now),
                    i.isFinalReportOverdue(now),
                    i.getSeverity().isSignificant());
        }
    }
}
