package com.openlab.qualitos.quality.breach.application;

import com.openlab.qualitos.quality.breach.domain.BreachIncident;
import com.openlab.qualitos.quality.breach.domain.BreachSeverity;
import com.openlab.qualitos.quality.breach.domain.BreachStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class BreachDto {

    private BreachDto() {}

    public record DetectRequest(
            String internalReference,
            String title,
            String description,
            Instant detectedAt,
            Instant occurredAt,
            BreachSeverity severity,
            long affectedSubjectsCount,
            Set<String> affectedDataCategories,
            String riskOfHarmDescription,
            UUID reportedByUserId) {}

    public record StartAssessmentRequest(UUID handledByUserId) {}
    public record ContainRequest(String containmentMeasures, UUID handledByUserId) {}
    public record DpaNotificationRequest(Instant notifiedAt, String reference) {}
    public record SubjectsNotificationRequest(Instant notifiedAt, String channel) {}
    public record CloseRequest(String closureNotes) {}
    public record RejectRequest(String reason) {}
    public record UpdateSeverityRequest(BreachSeverity severity) {}

    public record View(
            UUID id, UUID tenantId,
            String internalReference, String title, String description,
            Instant detectedAt, Instant occurredAt, Instant dpaDeadlineAt,
            BreachSeverity severity, BreachStatus status,
            long affectedSubjectsCount, Set<String> affectedDataCategories,
            String riskOfHarmDescription, String containmentMeasures,
            Instant dpaNotifiedAt, String dpaReference,
            Instant subjectsNotifiedAt, String subjectsNotificationChannel,
            String rejectionReason, String closureNotes,
            UUID reportedByUserId, UUID handledByUserId,
            Instant closedAt, Instant updatedAt,
            boolean dpaOverdue, boolean subjectNotificationRequired
    ) {
        public static View of(BreachIncident i, Instant now) {
            return new View(
                    i.getId(), i.getTenantId(),
                    i.getInternalReference(), i.getTitle(), i.getDescription(),
                    i.getDetectedAt(), i.getOccurredAt(), i.getDpaDeadlineAt(),
                    i.getSeverity(), i.getStatus(),
                    i.getAffectedSubjectsCount(), i.getAffectedDataCategories(),
                    i.getRiskOfHarmDescription(), i.getContainmentMeasures(),
                    i.getDpaNotifiedAt(), i.getDpaReference(),
                    i.getSubjectsNotifiedAt(), i.getSubjectsNotificationChannel(),
                    i.getRejectionReason(), i.getClosureNotes(),
                    i.getReportedByUserId(), i.getHandledByUserId(),
                    i.getClosedAt(), i.getUpdatedAt(),
                    i.isDpaNotificationOverdue(now), i.isSubjectNotificationRequired());
        }
    }
}
