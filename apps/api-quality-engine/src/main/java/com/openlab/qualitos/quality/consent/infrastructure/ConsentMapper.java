package com.openlab.qualitos.quality.consent.infrastructure;

import com.openlab.qualitos.quality.consent.domain.Consent;

final class ConsentMapper {
    private ConsentMapper() {}

    static ConsentJpaEntity toEntity(Consent c, ConsentJpaEntity target) {
        ConsentJpaEntity e = target != null ? target : new ConsentJpaEntity();
        if (c.getId() != null) e.setId(c.getId());
        e.setTenantId(c.getTenantId());
        e.setSubjectIdentifierHash(c.getSubjectIdentifierHash());
        e.setSubjectIdentifierLabel(c.getSubjectIdentifierLabel());
        e.setPurposeCode(c.getPurposeCode());
        e.setPurposeVersion(c.getPurposeVersion());
        e.setSource(c.getSource());
        e.setEvidenceUrl(c.getEvidenceUrl());
        e.setIpAddress(c.getIpAddress());
        e.setUserAgent(c.getUserAgent());
        e.setGrantedByUserId(c.getGrantedByUserId());
        e.setGrantedAt(c.getGrantedAt());
        e.setExpiresAt(c.getExpiresAt());
        e.setStatus(c.getStatus());
        e.setWithdrawnAt(c.getWithdrawnAt());
        e.setWithdrawnByUserId(c.getWithdrawnByUserId());
        e.setWithdrawalReason(c.getWithdrawalReason());
        e.setUpdatedAt(c.getUpdatedAt());
        return e;
    }

    static Consent toDomain(ConsentJpaEntity e) {
        return new Consent(
                e.getId(), e.getTenantId(),
                e.getSubjectIdentifierHash(), e.getSubjectIdentifierLabel(),
                e.getPurposeCode(), e.getPurposeVersion(),
                e.getSource(), e.getEvidenceUrl(),
                e.getIpAddress(), e.getUserAgent(),
                e.getGrantedByUserId(), e.getGrantedAt(), e.getExpiresAt(),
                e.getStatus(), e.getWithdrawnAt(),
                e.getWithdrawnByUserId(), e.getWithdrawalReason(),
                e.getUpdatedAt());
    }
}
