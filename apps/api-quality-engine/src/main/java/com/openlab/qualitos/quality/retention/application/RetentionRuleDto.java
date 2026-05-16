package com.openlab.qualitos.quality.retention.application;

import com.openlab.qualitos.quality.retention.domain.RetentionRule;
import com.openlab.qualitos.quality.retention.domain.RetentionRuleStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public final class RetentionRuleDto {

    private RetentionRuleDto() {}

    public record CreateRequest(
            String dataCategoryCode,
            String dataCategoryLabel,
            Duration retentionPeriod,
            String legalBasis,
            String lawfulBasisReference,
            UUID createdByUserId) {}

    public record EditRequest(
            String dataCategoryLabel,
            Duration retentionPeriod,
            String legalBasis,
            String lawfulBasisReference) {}

    /** Calcul de date d'effacement pour un enregistrement donné. */
    public record ErasureEvaluation(
            String dataCategoryCode,
            Instant recordCreatedAt,
            Instant erasureAt,
            boolean dueNow,
            UUID ruleId,
            Duration retentionPeriod) {}

    public record View(
            UUID id, UUID tenantId,
            String dataCategoryCode, String dataCategoryLabel,
            Duration retentionPeriod,
            String legalBasis, String lawfulBasisReference,
            RetentionRuleStatus status,
            Instant effectiveFrom, Instant effectiveTo,
            UUID createdByUserId, Instant createdAt, Instant updatedAt
    ) {
        public static View of(RetentionRule r) {
            return new View(
                    r.getId(), r.getTenantId(),
                    r.getDataCategoryCode(), r.getDataCategoryLabel(),
                    r.getRetentionPeriod(),
                    r.getLegalBasis(), r.getLawfulBasisReference(),
                    r.getStatus(), r.getEffectiveFrom(), r.getEffectiveTo(),
                    r.getCreatedByUserId(), r.getCreatedAt(), r.getUpdatedAt());
        }
    }
}
