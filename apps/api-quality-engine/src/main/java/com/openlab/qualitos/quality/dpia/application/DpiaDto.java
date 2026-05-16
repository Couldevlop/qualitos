package com.openlab.qualitos.quality.dpia.application;

import com.openlab.qualitos.quality.dpia.domain.Dpia;
import com.openlab.qualitos.quality.dpia.domain.DpiaStatus;
import com.openlab.qualitos.quality.dpia.domain.RiskLevel;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class DpiaDto {

    private DpiaDto() {}

    public record CreateRequest(
            String reference,
            String title,
            String description,
            Set<UUID> linkedProcessingActivityIds,
            RiskLevel initialRiskLevel,
            UUID createdByUserId) {}

    public record EditRequest(
            String title,
            String description,
            Set<UUID> linkedProcessingActivityIds,
            String necessityAndProportionalityNotes,
            String risksToRightsAndFreedoms,
            String mitigationMeasures,
            RiskLevel overallRiskLevel,
            boolean consultationRequired,
            String consultationNotes) {}

    public record StartRequest(UUID handledByUserId) {}
    public record OpinionRequest(UUID dpoUserId, String dpoOpinion) {}

    public record View(
            UUID id, UUID tenantId, String reference, String title, String description,
            Set<UUID> linkedProcessingActivityIds,
            String necessityAndProportionalityNotes,
            String risksToRightsAndFreedoms,
            String mitigationMeasures,
            RiskLevel overallRiskLevel,
            boolean consultationRequired, String consultationNotes,
            DpiaStatus status,
            UUID dpoUserId, String dpoOpinion, Instant dpoOpinionAt,
            Instant effectiveFrom, Instant effectiveTo,
            UUID createdByUserId, UUID handledByUserId,
            Instant createdAt, Instant updatedAt
    ) {
        public static View of(Dpia d) {
            return new View(
                    d.getId(), d.getTenantId(), d.getReference(),
                    d.getTitle(), d.getDescription(),
                    d.getLinkedProcessingActivityIds(),
                    d.getNecessityAndProportionalityNotes(),
                    d.getRisksToRightsAndFreedoms(),
                    d.getMitigationMeasures(),
                    d.getOverallRiskLevel(),
                    d.isConsultationRequired(), d.getConsultationNotes(),
                    d.getStatus(),
                    d.getDpoUserId(), d.getDpoOpinion(), d.getDpoOpinionAt(),
                    d.getEffectiveFrom(), d.getEffectiveTo(),
                    d.getCreatedByUserId(), d.getHandledByUserId(),
                    d.getCreatedAt(), d.getUpdatedAt());
        }
    }
}
