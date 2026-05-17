package com.openlab.qualitos.quality.aiconformity.infrastructure;

import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessment;

final class ConformityAssessmentMapper {
    private ConformityAssessmentMapper() {}

    static ConformityAssessmentJpaEntity toEntity(
            ConformityAssessment a, ConformityAssessmentJpaEntity target) {
        ConformityAssessmentJpaEntity e = target != null ? target : new ConformityAssessmentJpaEntity();
        if (a.getId() != null) e.setId(a.getId());
        e.setTenantId(a.getTenantId());
        e.setReference(a.getReference());
        e.setAiSystemId(a.getAiSystemId());
        e.setQmsId(a.getQmsId());
        e.setProcedure(a.getProcedure());
        e.setNotifiedBodyId(a.getNotifiedBodyId());
        e.setNotifiedBodyName(a.getNotifiedBodyName());
        e.setScope(a.getScope());
        e.setStatus(a.getStatus());
        e.setPlannedAt(a.getPlannedAt());
        e.setStartedAt(a.getStartedAt());
        e.setCertifiedAt(a.getCertifiedAt());
        e.setCertificateNumber(a.getCertificateNumber());
        e.setValidUntil(a.getValidUntil());
        e.setEuDeclarationReference(a.getEuDeclarationReference());
        e.setExpiredAt(a.getExpiredAt());
        e.setRevokedAt(a.getRevokedAt());
        e.setRevocationReason(a.getRevocationReason());
        e.setFailedAt(a.getFailedAt());
        e.setFailureReason(a.getFailureReason());
        e.setCreatedByUserId(a.getCreatedByUserId());
        e.setCreatedAt(a.getCreatedAt());
        e.setUpdatedAt(a.getUpdatedAt());
        return e;
    }

    static ConformityAssessment toDomain(ConformityAssessmentJpaEntity e) {
        return new ConformityAssessment(
                e.getId(), e.getTenantId(), e.getReference(), e.getAiSystemId(),
                e.getQmsId(), e.getProcedure(),
                e.getNotifiedBodyId(), e.getNotifiedBodyName(), e.getScope(),
                e.getStatus(),
                e.getPlannedAt(), e.getStartedAt(),
                e.getCertifiedAt(), e.getCertificateNumber(),
                e.getValidUntil(), e.getEuDeclarationReference(),
                e.getExpiredAt(),
                e.getRevokedAt(), e.getRevocationReason(),
                e.getFailedAt(), e.getFailureReason(),
                e.getCreatedByUserId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
