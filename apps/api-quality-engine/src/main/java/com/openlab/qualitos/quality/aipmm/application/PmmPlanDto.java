package com.openlab.qualitos.quality.aipmm.application;

import com.openlab.qualitos.quality.aipmm.domain.PmmPlan;
import com.openlab.qualitos.quality.aipmm.domain.PmmPlanStatus;
import com.openlab.qualitos.quality.aipmm.domain.PmmReviewFrequency;

import java.time.Instant;
import java.util.UUID;

public final class PmmPlanDto {

    private PmmPlanDto() {}

    public record DraftRequest(
            String reference, UUID aiSystemId,
            String name, String description,
            String metricsMonitored, String collectionMethod,
            PmmReviewFrequency reviewFrequency,
            String responsiblePartyDescription, String triggerCriteria,
            String qmsLinkReference, UUID createdByUserId) {}

    public record EditRequest(
            String name, String description,
            String metricsMonitored, String collectionMethod,
            PmmReviewFrequency reviewFrequency,
            String responsiblePartyDescription, String triggerCriteria,
            String qmsLinkReference) {}

    public record ReviewRequest(UUID reviewedByUserId) {}

    public record SuspendRequest(String reason) {}

    public record CloseRequest(String reason) {}

    public record View(
            UUID id, UUID tenantId, String reference, UUID aiSystemId,
            String name, String description,
            String metricsMonitored, String collectionMethod,
            PmmReviewFrequency reviewFrequency,
            String responsiblePartyDescription, String triggerCriteria,
            String qmsLinkReference,
            PmmPlanStatus status,
            Instant activatedAt,
            Instant lastReviewedAt, UUID lastReviewedByUserId,
            Instant nextReviewDueAt,
            Instant suspendedAt, String suspensionReason,
            Instant effectiveTo, String closureReason,
            UUID createdByUserId, Instant createdAt, Instant updatedAt
    ) {
        public static View of(PmmPlan p) {
            return new View(
                    p.getId(), p.getTenantId(), p.getReference(), p.getAiSystemId(),
                    p.getName(), p.getDescription(),
                    p.getMetricsMonitored(), p.getCollectionMethod(),
                    p.getReviewFrequency(),
                    p.getResponsiblePartyDescription(), p.getTriggerCriteria(),
                    p.getQmsLinkReference(),
                    p.getStatus(),
                    p.getActivatedAt(),
                    p.getLastReviewedAt(), p.getLastReviewedByUserId(),
                    p.nextReviewDueAt(),
                    p.getSuspendedAt(), p.getSuspensionReason(),
                    p.getEffectiveTo(), p.getClosureReason(),
                    p.getCreatedByUserId(), p.getCreatedAt(), p.getUpdatedAt());
        }
    }
}
