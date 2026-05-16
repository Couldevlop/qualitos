package com.openlab.qualitos.quality.aiact.web;

import com.openlab.qualitos.quality.aiact.domain.AiRiskClassification;
import com.openlab.qualitos.quality.aiact.domain.AiSystemRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

public final class AiSystemWebDto {

    private AiSystemWebDto() {}

    public record DraftRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Z][A-Z0-9_-]{1,63}$",
                    message = "reference must match [A-Z][A-Z0-9_-]{1,63}")
            String reference,
            @NotBlank @Size(max = 250) String name,
            @Size(max = 4000) String description,
            @Size(max = 250) String providerName,
            @NotBlank @Size(max = 4000) String intendedPurpose,
            @NotNull AiRiskClassification riskClassification,
            @NotNull AiSystemRole role,
            boolean generalPurpose,
            @Size(max = 1024) String conformityAssessmentEvidenceUrl,
            @Size(max = 250) String ceMarkingNumber,
            @Size(max = 4000) String humanOversightDescription,
            @Size(max = 4000) String transparencyMeasures,
            @Size(max = 4000) String dataGovernanceNotes,
            UUID linkedDpiaId,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedAutomatedDecisionIds,
            @NotNull UUID createdByUserId) {}

    public record EditRequest(
            @NotBlank @Size(max = 250) String name,
            @Size(max = 4000) String description,
            @Size(max = 250) String providerName,
            @NotBlank @Size(max = 4000) String intendedPurpose,
            @NotNull AiRiskClassification riskClassification,
            @NotNull AiSystemRole role,
            boolean generalPurpose,
            @Size(max = 1024) String conformityAssessmentEvidenceUrl,
            @Size(max = 250) String ceMarkingNumber,
            @Size(max = 4000) String humanOversightDescription,
            @Size(max = 4000) String transparencyMeasures,
            @Size(max = 4000) String dataGovernanceNotes,
            UUID linkedDpiaId,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedAutomatedDecisionIds) {}

    public record WithdrawRequest(@NotBlank @Size(max = 2000) String reason) {}
}
