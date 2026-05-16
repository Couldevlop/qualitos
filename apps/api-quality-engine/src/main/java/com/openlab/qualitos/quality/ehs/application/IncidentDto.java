package com.openlab.qualitos.quality.ehs.application;

import com.openlab.qualitos.quality.ehs.domain.Incident;
import com.openlab.qualitos.quality.ehs.domain.IncidentSeverity;
import com.openlab.qualitos.quality.ehs.domain.IncidentStatus;
import com.openlab.qualitos.quality.ehs.domain.IncidentType;

import java.time.Instant;
import java.util.UUID;

/**
 * DTOs de la frontière application — découpe entre l'agrégat domaine et la
 * couche web. La couche web peut les exposer telles quelles ou les remapper.
 */
public final class IncidentDto {

    private IncidentDto() {}

    public record ReportRequest(
            String code,
            String title,
            String description,
            IncidentType type,
            IncidentSeverity severity,
            Instant occurredAt,
            String location,
            UUID reportedBy
    ) {}

    public record EditRequest(
            String title,
            String description,
            String location,
            String personsInvolved,
            IncidentSeverity severity,
            String standardsCsv
    ) {}

    public record InvestigateRequest(UUID ownerUserId) {}

    public record MitigateRequest(String rootCause, String correctiveActions) {}

    public record LinkCapaRequest(UUID capaCaseId) {}
    public record LinkNcRequest(UUID ncId) {}

    public record IncidentView(
            UUID id, UUID tenantId, String code, String title, String description,
            IncidentType type, IncidentSeverity severity, IncidentStatus status,
            Instant occurredAt, Instant reportedAt,
            Instant mitigatedAt, Instant closedAt,
            String location, String personsInvolved,
            String rootCause, String correctiveActions, String standardsCsv,
            UUID capaCaseId, UUID ncId, UUID ownerUserId, UUID reportedBy,
            Instant createdAt, Instant updatedAt
    ) {
        public static IncidentView of(Incident i) {
            return new IncidentView(
                    i.getId(), i.getTenantId(), i.getCode(), i.getTitle(), i.getDescription(),
                    i.getType(), i.getSeverity(), i.getStatus(),
                    i.getOccurredAt(), i.getReportedAt(),
                    i.getMitigatedAt(), i.getClosedAt(),
                    i.getLocation(), i.getPersonsInvolved(),
                    i.getRootCause(), i.getCorrectiveActions(), i.getStandardsCsv(),
                    i.getCapaCaseId(), i.getNcId(), i.getOwnerUserId(), i.getReportedBy(),
                    i.getCreatedAt(), i.getUpdatedAt());
        }
    }
}
