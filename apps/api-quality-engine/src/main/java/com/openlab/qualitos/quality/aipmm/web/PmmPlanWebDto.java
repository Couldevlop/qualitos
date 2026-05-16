package com.openlab.qualitos.quality.aipmm.web;

import com.openlab.qualitos.quality.aipmm.domain.PmmReviewFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public final class PmmPlanWebDto {

    private PmmPlanWebDto() {}

    public record DraftRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotNull UUID aiSystemId,
            @NotBlank @Size(max = 250) String name,
            @Size(max = 4000) String description,
            @Size(max = 4000) String metricsMonitored,
            @Size(max = 4000) String collectionMethod,
            PmmReviewFrequency reviewFrequency,
            @Size(max = 4000) String responsiblePartyDescription,
            @Size(max = 4000) String triggerCriteria,
            @Size(max = 250) String qmsLinkReference,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @NotBlank @Size(max = 250) String name,
            @Size(max = 4000) String description,
            @Size(max = 4000) String metricsMonitored,
            @Size(max = 4000) String collectionMethod,
            PmmReviewFrequency reviewFrequency,
            @Size(max = 4000) String responsiblePartyDescription,
            @Size(max = 4000) String triggerCriteria,
            @Size(max = 250) String qmsLinkReference) {}

    public record ReviewRequest(@NotNull UUID reviewedByUserId) {}

    public record SuspendRequest(@NotBlank @Size(max = 2000) String reason) {}

    public record CloseRequest(@NotBlank @Size(max = 2000) String reason) {}
}
