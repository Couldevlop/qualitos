package com.openlab.qualitos.quality.retention.infrastructure;

import com.openlab.qualitos.quality.retention.domain.RetentionRule;

import java.time.Duration;

final class RetentionRuleMapper {
    private RetentionRuleMapper() {}

    static RetentionRuleJpaEntity toEntity(RetentionRule r, RetentionRuleJpaEntity target) {
        RetentionRuleJpaEntity e = target != null ? target : new RetentionRuleJpaEntity();
        if (r.getId() != null) e.setId(r.getId());
        e.setTenantId(r.getTenantId());
        e.setDataCategoryCode(r.getDataCategoryCode());
        e.setDataCategoryLabel(r.getDataCategoryLabel());
        e.setRetentionPeriodSeconds(r.getRetentionPeriod().getSeconds());
        e.setLegalBasis(r.getLegalBasis());
        e.setLawfulBasisReference(r.getLawfulBasisReference());
        e.setStatus(r.getStatus());
        e.setEffectiveFrom(r.getEffectiveFrom());
        e.setEffectiveTo(r.getEffectiveTo());
        e.setCreatedByUserId(r.getCreatedByUserId());
        e.setCreatedAt(r.getCreatedAt());
        e.setUpdatedAt(r.getUpdatedAt());
        return e;
    }

    static RetentionRule toDomain(RetentionRuleJpaEntity e) {
        return new RetentionRule(
                e.getId(), e.getTenantId(),
                e.getDataCategoryCode(), e.getDataCategoryLabel(),
                Duration.ofSeconds(e.getRetentionPeriodSeconds()),
                e.getLegalBasis(), e.getLawfulBasisReference(),
                e.getStatus(), e.getEffectiveFrom(), e.getEffectiveTo(),
                e.getCreatedByUserId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
