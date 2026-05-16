package com.openlab.qualitos.quality.breach.web;

import com.openlab.qualitos.quality.breach.domain.BreachSeverity;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class BreachWebDto {

    private BreachWebDto() {}

    public record DetectRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "internalReference must match [A-Z][A-Z0-9_-]{1,63}")
            String internalReference,
            @NotBlank @Size(max = 250) String title,
            @Size(max = 4000) String description,
            @NotNull Instant detectedAt,
            Instant occurredAt,
            @NotNull BreachSeverity severity,
            @Min(0) long affectedSubjectsCount,
            Set<String> affectedDataCategories,
            @Size(max = 2000) String riskOfHarmDescription,
            @NotNull UUID reportedByUserId) {}

    public record StartAssessmentRequest(@NotNull UUID handledByUserId) {}

    public record ContainRequest(
            @NotBlank @Size(max = 4000) String containmentMeasures,
            UUID handledByUserId) {}

    public record DpaNotificationRequest(
            @NotNull Instant notifiedAt,
            @NotBlank @Size(max = 250) String reference) {}

    public record SubjectsNotificationRequest(
            @NotNull Instant notifiedAt,
            @NotBlank @Size(max = 250) String channel) {}

    public record CloseRequest(@Size(max = 4000) String closureNotes) {}

    public record RejectRequest(@NotBlank @Size(max = 2000) String reason) {}

    public record UpdateSeverityRequest(@NotNull BreachSeverity severity) {}
}
