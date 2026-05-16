package com.openlab.qualitos.quality.gdpr.infrastructure;

import com.openlab.qualitos.quality.gdpr.domain.DataSubjectRequest;

final class SubjectRequestMapper {
    private SubjectRequestMapper() {}

    static SubjectRequestJpaEntity toEntity(DataSubjectRequest d, SubjectRequestJpaEntity target) {
        SubjectRequestJpaEntity e = target != null ? target : new SubjectRequestJpaEntity();
        if (d.getId() != null) e.setId(d.getId());
        e.setTenantId(d.getTenantId());
        e.setType(d.getType());
        e.setSubjectIdentifierHash(d.getSubjectIdentifierHash());
        e.setSubjectIdentifierLabel(d.getSubjectIdentifierLabel());
        e.setStatus(d.getStatus());
        e.setReceivedAt(d.getReceivedAt());
        e.setDeadlineAt(d.getDeadlineAt());
        e.setExtended(d.isExtended());
        e.setInProgressAt(d.getInProgressAt());
        e.setCompletedAt(d.getCompletedAt());
        e.setRejectionReason(d.getRejectionReason());
        e.setResolutionNotes(d.getResolutionNotes());
        e.setEvidenceUrl(d.getEvidenceUrl());
        e.setRequestedByUserId(d.getRequestedByUserId());
        e.setHandledByUserId(d.getHandledByUserId());
        e.setUpdatedAt(d.getUpdatedAt());
        return e;
    }

    static DataSubjectRequest toDomain(SubjectRequestJpaEntity e) {
        return new DataSubjectRequest(
                e.getId(), e.getTenantId(), e.getType(),
                e.getSubjectIdentifierHash(), e.getSubjectIdentifierLabel(),
                e.getStatus(), e.getReceivedAt(), e.getDeadlineAt(),
                e.isExtended(), e.getInProgressAt(), e.getCompletedAt(),
                e.getRejectionReason(), e.getResolutionNotes(), e.getEvidenceUrl(),
                e.getRequestedByUserId(), e.getHandledByUserId(), e.getUpdatedAt());
    }
}
