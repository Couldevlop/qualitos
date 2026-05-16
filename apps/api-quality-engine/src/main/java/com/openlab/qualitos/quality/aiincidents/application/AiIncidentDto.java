package com.openlab.qualitos.quality.aiincidents.application;

import com.openlab.qualitos.quality.aiincidents.domain.AiIncident;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentSeverity;
import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentStatus;

import java.time.Instant;
import java.util.UUID;

public final class AiIncidentDto {

    private AiIncidentDto() {}

    public record DetectRequest(
            String reference, UUID aiSystemId,
            AiIncidentSeverity severity, String description,
            String affectedPersonsDescription, String immediateActionsTaken,
            Instant occurredAt, Instant detectedAt,
            UUID createdByUserId) {}

    public record EditRequest(
            String description, String affectedPersonsDescription,
            String immediateActionsTaken) {}

    public record StartInvestigationRequest(UUID investigationLeadUserId) {}

    public record NotifyRegulatorRequest(
            String regulatorReference, String rootCauseAnalysis,
            String correctiveActions) {}

    public record CloseRequest(String correctiveActions) {}

    public record DismissRequest(String reason) {}

    public record View(
            UUID id, UUID tenantId, String reference, UUID aiSystemId,
            AiIncidentSeverity severity, String description,
            String affectedPersonsDescription, String immediateActionsTaken,
            Instant occurredAt, Instant detectedAt,
            AiIncidentStatus status,
            Instant investigationStartedAt, UUID investigationLeadUserId,
            String rootCauseAnalysis, String correctiveActions,
            Instant notifiedRegulatorAt, String regulatorReference,
            Instant closedAt, Instant dismissedAt, String dismissalReason,
            Instant regulatorNotificationDueAt,
            UUID createdByUserId, Instant createdAt, Instant updatedAt
    ) {
        public static View of(AiIncident i) {
            return new View(
                    i.getId(), i.getTenantId(), i.getReference(), i.getAiSystemId(),
                    i.getSeverity(), i.getDescription(),
                    i.getAffectedPersonsDescription(), i.getImmediateActionsTaken(),
                    i.getOccurredAt(), i.getDetectedAt(),
                    i.getStatus(),
                    i.getInvestigationStartedAt(), i.getInvestigationLeadUserId(),
                    i.getRootCauseAnalysis(), i.getCorrectiveActions(),
                    i.getNotifiedRegulatorAt(), i.getRegulatorReference(),
                    i.getClosedAt(), i.getDismissedAt(), i.getDismissalReason(),
                    i.regulatorNotificationDueAt(),
                    i.getCreatedByUserId(), i.getCreatedAt(), i.getUpdatedAt());
        }
    }
}
