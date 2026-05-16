package com.openlab.qualitos.quality.automateddecisions.application;

import com.openlab.qualitos.quality.automateddecisions.domain.Art22LawfulBasis;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionRecord;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionStatus;
import com.openlab.qualitos.quality.automateddecisions.domain.AutomatedDecisionType;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class AutomatedDecisionDto {

    private AutomatedDecisionDto() {}

    public record CreateRequest(
            String reference,
            String name,
            String description,
            AutomatedDecisionType decisionType,
            Art22LawfulBasis art22LawfulBasis,
            String lawfulBasisDetails,
            Set<String> inputDataCategories,
            Set<UUID> linkedProcessingActivityIds,
            UUID linkedDpiaId,
            String algorithmDescription,
            String significanceForSubject,
            String humanReviewMechanism,
            String objectionMechanism,
            UUID createdByUserId) {}

    public record EditRequest(
            String name,
            String description,
            AutomatedDecisionType decisionType,
            Art22LawfulBasis art22LawfulBasis,
            String lawfulBasisDetails,
            Set<String> inputDataCategories,
            Set<UUID> linkedProcessingActivityIds,
            UUID linkedDpiaId,
            String algorithmDescription,
            String significanceForSubject,
            String humanReviewMechanism,
            String objectionMechanism) {}

    public record View(
            UUID id, UUID tenantId, String reference, String name, String description,
            AutomatedDecisionType decisionType,
            Art22LawfulBasis art22LawfulBasis, String lawfulBasisDetails,
            Set<String> inputDataCategories,
            Set<UUID> linkedProcessingActivityIds, UUID linkedDpiaId,
            String algorithmDescription, String significanceForSubject,
            String humanReviewMechanism, String objectionMechanism,
            AutomatedDecisionStatus status,
            Instant effectiveFrom, Instant effectiveTo,
            UUID createdByUserId, Instant createdAt, Instant updatedAt
    ) {
        public static View of(AutomatedDecisionRecord r) {
            return new View(
                    r.getId(), r.getTenantId(), r.getReference(),
                    r.getName(), r.getDescription(),
                    r.getDecisionType(), r.getArt22LawfulBasis(), r.getLawfulBasisDetails(),
                    r.getInputDataCategories(),
                    r.getLinkedProcessingActivityIds(), r.getLinkedDpiaId(),
                    r.getAlgorithmDescription(), r.getSignificanceForSubject(),
                    r.getHumanReviewMechanism(), r.getObjectionMechanism(),
                    r.getStatus(), r.getEffectiveFrom(), r.getEffectiveTo(),
                    r.getCreatedByUserId(), r.getCreatedAt(), r.getUpdatedAt());
        }
    }
}
