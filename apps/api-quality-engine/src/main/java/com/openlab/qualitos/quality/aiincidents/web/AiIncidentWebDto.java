package com.openlab.qualitos.quality.aiincidents.web;

import com.openlab.qualitos.quality.aiincidents.domain.AiIncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class AiIncidentWebDto {

    private AiIncidentWebDto() {}

    public record DetectRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotNull UUID aiSystemId,
            @NotNull AiIncidentSeverity severity,
            @NotBlank @Size(max = 4000) String description,
            @Size(max = 4000) String affectedPersonsDescription,
            @Size(max = 4000) String immediateActionsTaken,
            @NotNull Instant occurredAt,
            @NotNull Instant detectedAt,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @NotBlank @Size(max = 4000) String description,
            @Size(max = 4000) String affectedPersonsDescription,
            @Size(max = 4000) String immediateActionsTaken) {}

    public record StartInvestigationRequest(@NotNull UUID investigationLeadUserId) {}

    public record NotifyRegulatorRequest(
            @NotBlank @Size(max = 250) String regulatorReference,
            @NotBlank @Size(max = 4000) String rootCauseAnalysis,
            @Size(max = 4000) String correctiveActions) {}

    public record CloseRequest(
            @NotBlank @Size(max = 4000) String correctiveActions) {}

    public record DismissRequest(@NotBlank @Size(max = 2000) String reason) {}
}
