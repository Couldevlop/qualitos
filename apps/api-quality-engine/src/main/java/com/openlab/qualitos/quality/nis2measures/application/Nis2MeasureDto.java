package com.openlab.qualitos.quality.nis2measures.application;

import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureCategory;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2MeasureStatus;
import com.openlab.qualitos.quality.nis2measures.domain.Nis2RiskMeasure;
import com.openlab.qualitos.quality.nis2measures.domain.ResidualRiskRating;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class Nis2MeasureDto {

    private Nis2MeasureDto() {}

    public record PlanRequest(
            String reference, Nis2MeasureCategory category,
            String title, String description,
            UUID ownerUserId, int maturityLevel,
            ResidualRiskRating residualRiskRating, String criticalRiskJustification,
            int reviewIntervalDays,
            Set<String> evidenceUrls,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedProcessorAgreementIds,
            String notes,
            UUID createdByUserId) {}

    public record EditRequest(
            String title, String description,
            UUID ownerUserId, int maturityLevel,
            ResidualRiskRating residualRiskRating, String criticalRiskJustification,
            int reviewIntervalDays,
            Set<String> evidenceUrls,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedProcessorAgreementIds,
            String notes) {}

    public record VerifyRequest(UUID reviewedByUserId, Instant reviewedAt) {}
    public record ReviewRequest(UUID reviewedByUserId, Instant reviewedAt) {}

    public record View(
            UUID id, UUID tenantId, String reference,
            Nis2MeasureCategory category, String title, String description,
            Nis2MeasureStatus status, UUID ownerUserId,
            int maturityLevel,
            ResidualRiskRating residualRiskRating, String criticalRiskJustification,
            int reviewIntervalDays,
            Instant effectiveFrom, Instant effectiveTo,
            Instant lastReviewedAt, UUID reviewedByUserId, Instant nextReviewDueAt,
            Set<String> evidenceUrls,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedProcessorAgreementIds,
            String notes,
            UUID createdByUserId, Instant createdAt, Instant updatedAt,
            boolean reviewOverdue, boolean criticalResidualRisk
    ) {
        public static View of(Nis2RiskMeasure m, Instant now) {
            return new View(
                    m.getId(), m.getTenantId(), m.getReference(),
                    m.getCategory(), m.getTitle(), m.getDescription(),
                    m.getStatus(), m.getOwnerUserId(),
                    m.getMaturityLevel(),
                    m.getResidualRiskRating(), m.getCriticalRiskJustification(),
                    m.getReviewIntervalDays(),
                    m.getEffectiveFrom(), m.getEffectiveTo(),
                    m.getLastReviewedAt(), m.getReviewedByUserId(), m.getNextReviewDueAt(),
                    m.getEvidenceUrls(),
                    m.getLinkedProcessingActivityIds(),
                    m.getLinkedProcessorAgreementIds(),
                    m.getNotes(),
                    m.getCreatedByUserId(), m.getCreatedAt(), m.getUpdatedAt(),
                    m.isReviewOverdue(now),
                    m.getResidualRiskRating().requiresExecutiveAttention());
        }
    }
}
