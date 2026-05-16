package com.openlab.qualitos.quality.dpia.web;

import com.openlab.qualitos.quality.dpia.domain.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public final class DpiaWebDto {

    private DpiaWebDto() {}

    public record CreateRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotBlank @Size(max = 250) String title,
            @Size(max = 4000) String description,
            Set<UUID> linkedProcessingActivityIds,
            @NotNull RiskLevel initialRiskLevel,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @NotBlank @Size(max = 250) String title,
            @Size(max = 4000) String description,
            Set<UUID> linkedProcessingActivityIds,
            @Size(max = 8000) String necessityAndProportionalityNotes,
            @Size(max = 8000) String risksToRightsAndFreedoms,
            @Size(max = 8000) String mitigationMeasures,
            @NotNull RiskLevel overallRiskLevel,
            boolean consultationRequired,
            @Size(max = 8000) String consultationNotes) {}

    public record StartRequest(@NotNull UUID handledByUserId) {}

    public record OpinionRequest(
            @NotNull UUID dpoUserId,
            @NotBlank @Size(max = 8000) String dpoOpinion) {}
}
