package com.openlab.qualitos.quality.consent.application;

import com.openlab.qualitos.quality.consent.domain.Consent;
import com.openlab.qualitos.quality.consent.domain.ConsentSource;
import com.openlab.qualitos.quality.consent.domain.ConsentStatus;

import java.time.Instant;
import java.util.UUID;

public final class ConsentDto {

    private ConsentDto() {}

    /** subjectIdentifier en clair — sera HASHÉ par le service avant persistance.
     *  ipAddress / userAgent optionnels (preuve, RGPD-compatible). */
    public record GrantRequest(
            String subjectIdentifier,
            String subjectIdentifierLabel,
            String purposeCode,
            String purposeVersion,
            ConsentSource source,
            String evidenceUrl,
            String ipAddress,
            String userAgent,
            UUID grantedByUserId,
            Instant expiresAt) {}

    public record WithdrawRequest(UUID actorUserId, String reason) {}

    public record View(
            UUID id, UUID tenantId,
            String subjectIdentifierHash, String subjectIdentifierLabel,
            String purposeCode, String purposeVersion,
            ConsentSource source, String evidenceUrl,
            String ipAddress, String userAgent,
            UUID grantedByUserId, Instant grantedAt, Instant expiresAt,
            ConsentStatus status,
            Instant withdrawnAt, UUID withdrawnByUserId, String withdrawalReason,
            Instant updatedAt, boolean active
    ) {
        public static View of(Consent c, Instant now) {
            return new View(
                    c.getId(), c.getTenantId(),
                    c.getSubjectIdentifierHash(), c.getSubjectIdentifierLabel(),
                    c.getPurposeCode(), c.getPurposeVersion(),
                    c.getSource(), c.getEvidenceUrl(),
                    c.getIpAddress(), c.getUserAgent(),
                    c.getGrantedByUserId(), c.getGrantedAt(), c.getExpiresAt(),
                    c.getStatus(), c.getWithdrawnAt(),
                    c.getWithdrawnByUserId(), c.getWithdrawalReason(),
                    c.getUpdatedAt(), c.isActive(now));
        }
    }
}
