package com.openlab.qualitos.quality.nis2measures.web;

import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureCategory;
import com.openlab.qualitos.quality.nis2measures.domain.ResidualRiskRating;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class Nis2MeasureWebDto {

    private Nis2MeasureWebDto() {}

    public record PlanRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotNull Nis2MeasureCategory category,
            @NotBlank @Size(max = 250) String title,
            @Size(max = 4000) String description,
            UUID ownerUserId,
            @Min(1) @Max(5) int maturityLevel,
            @NotNull ResidualRiskRating residualRiskRating,
            @Size(max = 4000) String criticalRiskJustification,
            @Min(30) @Max(1095) int reviewIntervalDays,
            Set<String> evidenceUrls,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedProcessorAgreementIds,
            @Size(max = 4000) String notes,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @NotBlank @Size(max = 250) String title,
            @Size(max = 4000) String description,
            UUID ownerUserId,
            @Min(1) @Max(5) int maturityLevel,
            @NotNull ResidualRiskRating residualRiskRating,
            @Size(max = 4000) String criticalRiskJustification,
            @Min(30) @Max(1095) int reviewIntervalDays,
            Set<String> evidenceUrls,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedProcessorAgreementIds,
            @Size(max = 4000) String notes) {}

    public record VerifyRequest(
            @NotNull UUID reviewedByUserId,
            @NotNull Instant reviewedAt) {}

    public record ReviewRequest(
            @NotNull UUID reviewedByUserId,
            @NotNull Instant reviewedAt) {}
}
