package com.openlab.qualitos.quality.aieudb.application;

import com.openlab.qualitos.quality.aieudb.domain.EudbRegistration;
import com.openlab.qualitos.quality.aieudb.domain.EudbRegistrationStatus;

import java.time.Instant;
import java.util.UUID;

public final class EudbRegistrationDto {

    private EudbRegistrationDto() {}

    public record DraftRequest(
            String reference, UUID aiSystemId,
            String providerEntityName, String providerEuRepresentative,
            String memberStateOfReference, String intendedPurposeSummary,
            String technicalDocumentationReference,
            UUID createdByUserId) {}

    public record EditRequest(
            String providerEntityName, String providerEuRepresentative,
            String memberStateOfReference, String intendedPurposeSummary,
            String technicalDocumentationReference) {}

    public record SubmitRequest(UUID submittedByUserId) {}

    public record MarkRegisteredRequest(String eudbId, Instant registrationDate) {}

    public record DeclareUpdateRequest(String updateSummary, Instant updateDate) {}

    public record RejectRequest(String reason) {}

    public record RetireRequest(String reason) {}

    public record View(
            UUID id, UUID tenantId, String reference, UUID aiSystemId,
            String providerEntityName, String providerEuRepresentative,
            String memberStateOfReference, String intendedPurposeSummary,
            String technicalDocumentationReference,
            String eudbId,
            EudbRegistrationStatus status,
            Instant submittedAt, UUID submittedByUserId,
            Instant registrationDate,
            Instant lastUpdateDate, String lastUpdateSummary,
            Instant rejectedAt, String rejectionReason,
            Instant retiredAt, String retirementReason,
            UUID createdByUserId, Instant createdAt, Instant updatedAt
    ) {
        public static View of(EudbRegistration r) {
            return new View(
                    r.getId(), r.getTenantId(), r.getReference(), r.getAiSystemId(),
                    r.getProviderEntityName(), r.getProviderEuRepresentative(),
                    r.getMemberStateOfReference(), r.getIntendedPurposeSummary(),
                    r.getTechnicalDocumentationReference(),
                    r.getEudbId(),
                    r.getStatus(),
                    r.getSubmittedAt(), r.getSubmittedByUserId(),
                    r.getRegistrationDate(),
                    r.getLastUpdateDate(), r.getLastUpdateSummary(),
                    r.getRejectedAt(), r.getRejectionReason(),
                    r.getRetiredAt(), r.getRetirementReason(),
                    r.getCreatedByUserId(), r.getCreatedAt(), r.getUpdatedAt());
        }
    }
}
