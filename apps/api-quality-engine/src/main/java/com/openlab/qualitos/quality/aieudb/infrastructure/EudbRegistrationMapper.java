package com.openlab.qualitos.quality.aieudb.infrastructure;

import com.openlab.qualitos.quality.aieudb.domain.EudbRegistration;

final class EudbRegistrationMapper {
    private EudbRegistrationMapper() {}

    static EudbRegistrationJpaEntity toEntity(EudbRegistration r, EudbRegistrationJpaEntity target) {
        EudbRegistrationJpaEntity e = target != null ? target : new EudbRegistrationJpaEntity();
        if (r.getId() != null) e.setId(r.getId());
        e.setTenantId(r.getTenantId());
        e.setReference(r.getReference());
        e.setAiSystemId(r.getAiSystemId());
        e.setProviderEntityName(r.getProviderEntityName());
        e.setProviderEuRepresentative(r.getProviderEuRepresentative());
        e.setMemberStateOfReference(r.getMemberStateOfReference());
        e.setIntendedPurposeSummary(r.getIntendedPurposeSummary());
        e.setTechnicalDocumentationReference(r.getTechnicalDocumentationReference());
        e.setEudbId(r.getEudbId());
        e.setStatus(r.getStatus());
        e.setSubmittedAt(r.getSubmittedAt());
        e.setSubmittedByUserId(r.getSubmittedByUserId());
        e.setRegistrationDate(r.getRegistrationDate());
        e.setLastUpdateDate(r.getLastUpdateDate());
        e.setLastUpdateSummary(r.getLastUpdateSummary());
        e.setRejectedAt(r.getRejectedAt());
        e.setRejectionReason(r.getRejectionReason());
        e.setRetiredAt(r.getRetiredAt());
        e.setRetirementReason(r.getRetirementReason());
        e.setCreatedByUserId(r.getCreatedByUserId());
        e.setCreatedAt(r.getCreatedAt());
        e.setUpdatedAt(r.getUpdatedAt());
        return e;
    }

    static EudbRegistration toDomain(EudbRegistrationJpaEntity e) {
        return new EudbRegistration(
                e.getId(), e.getTenantId(), e.getReference(), e.getAiSystemId(),
                e.getProviderEntityName(), e.getProviderEuRepresentative(),
                e.getMemberStateOfReference(), e.getIntendedPurposeSummary(),
                e.getTechnicalDocumentationReference(),
                e.getEudbId(),
                e.getStatus(),
                e.getSubmittedAt(), e.getSubmittedByUserId(),
                e.getRegistrationDate(),
                e.getLastUpdateDate(), e.getLastUpdateSummary(),
                e.getRejectedAt(), e.getRejectionReason(),
                e.getRetiredAt(), e.getRetirementReason(),
                e.getCreatedByUserId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
