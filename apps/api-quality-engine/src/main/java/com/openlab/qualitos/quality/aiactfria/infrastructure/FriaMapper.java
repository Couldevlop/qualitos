package com.openlab.qualitos.quality.aiactfria.infrastructure;

import com.openlab.qualitos.quality.aiactfria.domain.Fria;

final class FriaMapper {
    private FriaMapper() {}

    static FriaJpaEntity toEntity(Fria f, FriaJpaEntity target) {
        FriaJpaEntity e = target != null ? target : new FriaJpaEntity();
        if (f.getId() != null) e.setId(f.getId());
        e.setTenantId(f.getTenantId());
        e.setReference(f.getReference());
        e.setAiSystemId(f.getAiSystemId());
        e.setProcessDescription(f.getProcessDescription());
        e.setDeploymentDurationDescription(f.getDeploymentDurationDescription());
        e.setAffectedPersonsCategories(f.getAffectedPersonsCategories());
        e.setSpecificRisks(f.getSpecificRisks());
        e.setMitigationMeasures(f.getMitigationMeasures());
        e.setHumanOversightMeasures(f.getHumanOversightMeasures());
        e.setComplaintMechanismDescription(f.getComplaintMechanismDescription());
        e.setStatus(f.getStatus());
        e.setSubmittedAt(f.getSubmittedAt());
        e.setSubmittedByUserId(f.getSubmittedByUserId());
        e.setApprovedAt(f.getApprovedAt());
        e.setApprovedByUserId(f.getApprovedByUserId());
        e.setApprovalNotes(f.getApprovalNotes());
        e.setEffectiveTo(f.getEffectiveTo());
        e.setArchivedReason(f.getArchivedReason());
        e.setCreatedByUserId(f.getCreatedByUserId());
        e.setCreatedAt(f.getCreatedAt());
        e.setUpdatedAt(f.getUpdatedAt());
        return e;
    }

    static Fria toDomain(FriaJpaEntity e) {
        return new Fria(
                e.getId(), e.getTenantId(), e.getReference(), e.getAiSystemId(),
                e.getProcessDescription(), e.getDeploymentDurationDescription(),
                e.getAffectedPersonsCategories(), e.getSpecificRisks(),
                e.getMitigationMeasures(), e.getHumanOversightMeasures(),
                e.getComplaintMechanismDescription(),
                e.getStatus(),
                e.getSubmittedAt(), e.getSubmittedByUserId(),
                e.getApprovedAt(), e.getApprovedByUserId(), e.getApprovalNotes(),
                e.getEffectiveTo(), e.getArchivedReason(),
                e.getCreatedByUserId(), e.getCreatedAt(), e.getUpdatedAt());
    }
}
