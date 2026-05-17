package com.openlab.qualitos.quality.aiconformity.application;

import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessment;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityAssessmentStatus;
import com.openlab.qualitos.quality.aiconformity.domain.ConformityProcedure;

import java.time.Instant;
import java.util.UUID;

public final class ConformityAssessmentDto {

    private ConformityAssessmentDto() {}

    public record PlanRequest(
            String reference, UUID aiSystemId, UUID qmsId,
            ConformityProcedure procedure,
            String notifiedBodyId, String notifiedBodyName,
            String scope, UUID createdByUserId) {}

    public record EditRequest(
            UUID qmsId,
            String notifiedBodyId, String notifiedBodyName,
            String scope) {}

    public record CertifyRequest(
            String certificateNumber,
            String euDeclarationReference,
            Instant validUntil) {}

    public record RevokeRequest(String reason) {}

    public record FailRequest(String reason) {}

    public record View(
            UUID id, UUID tenantId, String reference, UUID aiSystemId, UUID qmsId,
            ConformityProcedure procedure,
            String notifiedBodyId, String notifiedBodyName, String scope,
            ConformityAssessmentStatus status,
            Instant plannedAt, Instant startedAt,
            Instant certifiedAt, String certificateNumber,
            Instant validUntil, String euDeclarationReference,
            Instant expiredAt,
            Instant revokedAt, String revocationReason,
            Instant failedAt, String failureReason,
            UUID createdByUserId, Instant createdAt, Instant updatedAt
    ) {
        public static View of(ConformityAssessment a) {
            return new View(
                    a.getId(), a.getTenantId(), a.getReference(),
                    a.getAiSystemId(), a.getQmsId(),
                    a.getProcedure(),
                    a.getNotifiedBodyId(), a.getNotifiedBodyName(), a.getScope(),
                    a.getStatus(),
                    a.getPlannedAt(), a.getStartedAt(),
                    a.getCertifiedAt(), a.getCertificateNumber(),
                    a.getValidUntil(), a.getEuDeclarationReference(),
                    a.getExpiredAt(),
                    a.getRevokedAt(), a.getRevocationReason(),
                    a.getFailedAt(), a.getFailureReason(),
                    a.getCreatedByUserId(), a.getCreatedAt(), a.getUpdatedAt());
        }
    }
}
