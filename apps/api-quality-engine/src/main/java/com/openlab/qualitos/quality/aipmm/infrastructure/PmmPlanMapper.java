package com.openlab.qualitos.quality.aipmm.infrastructure;

import com.openlab.qualitos.quality.aipmm.domain.PmmPlan;

final class PmmPlanMapper {
    private PmmPlanMapper() {}

    static PmmPlanJpaEntity toEntity(PmmPlan p, PmmPlanJpaEntity target) {
        PmmPlanJpaEntity e = target != null ? target : new PmmPlanJpaEntity();
        if (p.getId() != null) e.setId(p.getId());
        e.setTenantId(p.getTenantId());
        e.setReference(p.getReference());
        e.setAiSystemId(p.getAiSystemId());
        e.setName(p.getName());
        e.setDescription(p.getDescription());
        e.setMetricsMonitored(p.getMetricsMonitored());
        e.setCollectionMethod(p.getCollectionMethod());
        e.setReviewFrequency(p.getReviewFrequency());
        e.setResponsiblePartyDescription(p.getResponsiblePartyDescription());
        e.setTriggerCriteria(p.getTriggerCriteria());
        e.setQmsLinkReference(p.getQmsLinkReference());
        e.setStatus(p.getStatus());
        e.setActivatedAt(p.getActivatedAt());
        e.setLastReviewedAt(p.getLastReviewedAt());
        e.setLastReviewedByUserId(p.getLastReviewedByUserId());
        e.setSuspendedAt(p.getSuspendedAt());
        e.setSuspensionReason(p.getSuspensionReason());
        e.setEffectiveTo(p.getEffectiveTo());
        e.setClosureReason(p.getClosureReason());
        e.setCreatedByUserId(p.getCreatedByUserId());
        e.setCreatedAt(p.getCreatedAt());
        e.setUpdatedAt(p.getUpdatedAt());
        return e;
    }

    static PmmPlan toDomain(PmmPlanJpaEntity e) {
        return new PmmPlan(
                e.getId(), e.getTenantId(), e.getReference(), e.getAiSystemId(),
                e.getName(), e.getDescription(),
                e.getMetricsMonitored(), e.getCollectionMethod(),
                e.getReviewFrequency(),
                e.getResponsiblePartyDescription(), e.getTriggerCriteria(),
                e.getQmsLinkReference(),
                e.getStatus(),
                e.getActivatedAt(),
                e.getLastReviewedAt(), e.getLastReviewedByUserId(),
                e.getSuspendedAt(), e.getSuspensionReason(),
                e.getEffectiveTo(), e.getClosureReason(),
                e.getCreatedByUserId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
