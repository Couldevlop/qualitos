package com.openlab.qualitos.quality.cyberincidents.web;

import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentSeverity;
import com.openlab.qualitos.quality.cyberincidents.domain.CyberIncidentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class CyberIncidentWebDto {

    private CyberIncidentWebDto() {}

    public record DetectRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotBlank @Size(max = 250) String title,
            @Size(max = 4000) String description,
            @NotNull Instant detectedAt,
            Instant occurredAt,
            @NotNull CyberIncidentType incidentType,
            @NotNull CyberIncidentSeverity severity,
            @Min(0) long estimatedAffectedUsers,
            Set<String> affectedAssets,
            Set<String> affectedServices,
            UUID linkedBreachId,
            @NotNull UUID reportedByUserId) {}

    public record StartAssessmentRequest(@NotNull UUID handledByUserId) {}

    public record MitigateRequest(
            @NotBlank @Size(max = 4000) String containmentMeasures,
            @Size(max = 4000) String impactDescription,
            UUID handledByUserId) {}

    public record NotificationRequest(
            @NotNull Instant sentAt,
            @NotBlank @Size(max = 250) String reference) {}

    public record CloseRequest(@Size(max = 4000) String closureNotes) {}
    public record RejectRequest(@NotBlank @Size(max = 2000) String reason) {}
    public record UpdateSeverityRequest(@NotNull CyberIncidentSeverity severity) {}
    public record LinkBreachRequest(@NotNull UUID breachId) {}
}
