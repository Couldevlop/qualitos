package com.openlab.qualitos.quality.aiact.application;

import com.openlab.qualitos.quality.aiact.domain.AiRiskClassification;
import com.openlab.qualitos.quality.aiact.domain.AiSystem;
import com.openlab.qualitos.quality.aiact.domain.AiSystemRole;
import com.openlab.qualitos.quality.aiact.domain.AiSystemStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class AiSystemDto {

    private AiSystemDto() {}

    public record DraftRequest(
            String reference, String name, String description,
            String providerName, String intendedPurpose,
            AiRiskClassification riskClassification, AiSystemRole role,
            boolean generalPurpose,
            String conformityAssessmentEvidenceUrl, String ceMarkingNumber,
            String humanOversightDescription, String transparencyMeasures,
            String dataGovernanceNotes,
            UUID linkedDpiaId,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedAutomatedDecisionIds,
            UUID createdByUserId) {}

    public record EditRequest(
            String name, String description,
            String providerName, String intendedPurpose,
            AiRiskClassification riskClassification, AiSystemRole role,
            boolean generalPurpose,
            String conformityAssessmentEvidenceUrl, String ceMarkingNumber,
            String humanOversightDescription, String transparencyMeasures,
            String dataGovernanceNotes,
            UUID linkedDpiaId,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedAutomatedDecisionIds) {}

    public record WithdrawRequest(String reason) {}

    public record View(
            UUID id, UUID tenantId, String reference, String name, String description,
            String providerName, String intendedPurpose,
            AiRiskClassification riskClassification, AiSystemRole role,
            boolean generalPurpose,
            AiSystemStatus status,
            String conformityAssessmentEvidenceUrl, String ceMarkingNumber,
            String humanOversightDescription, String transparencyMeasures,
            String dataGovernanceNotes,
            UUID linkedDpiaId,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedAutomatedDecisionIds,
            Instant effectiveFrom, Instant effectiveTo, String withdrawalReason,
            UUID createdByUserId, Instant createdAt, Instant updatedAt,
            boolean prohibited, boolean requiresConformityAssessment, boolean requiresTransparency
    ) {
        public static View of(AiSystem s) {
            return new View(
                    s.getId(), s.getTenantId(), s.getReference(),
                    s.getName(), s.getDescription(),
                    s.getProviderName(), s.getIntendedPurpose(),
                    s.getRiskClassification(), s.getRole(), s.isGeneralPurpose(),
                    s.getStatus(),
                    s.getConformityAssessmentEvidenceUrl(), s.getCeMarkingNumber(),
                    s.getHumanOversightDescription(), s.getTransparencyMeasures(),
                    s.getDataGovernanceNotes(),
                    s.getLinkedDpiaId(),
                    s.getLinkedProcessingActivityIds(),
                    s.getLinkedAutomatedDecisionIds(),
                    s.getEffectiveFrom(), s.getEffectiveTo(), s.getWithdrawalReason(),
                    s.getCreatedByUserId(), s.getCreatedAt(), s.getUpdatedAt(),
                    s.getRiskClassification().isProhibited(),
                    s.getRiskClassification().requiresConformityAssessment(),
                    s.getRiskClassification().requiresTransparency());
        }
    }
}
