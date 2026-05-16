package com.openlab.qualitos.quality.processoragreements.application;

import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreement;
import com.openlab.qualitos.quality.processoragreements.domain.ProcessorAgreementStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class ProcessorAgreementDto {

    private ProcessorAgreementDto() {}

    public record CreateRequest(
            String reference,
            String processorName,
            String processorLegalEntity,
            String processorContact,
            String processorDpoContact,
            String processorCountry,
            String servicesDescription,
            Set<String> subProcessorCategories,
            Set<UUID> linkedProcessingActivityIds,
            Set<String> thirdCountryTransfers,
            String transferSafeguards,
            String contractDocumentUrl,
            Instant signedAt,
            Instant effectiveFrom,
            Instant expirationDate,
            String securityMeasures,
            int breachNotificationCommitmentHours,
            boolean auditRights,
            String auditRightsNotes,
            String dataReturnOrDeletionTerms,
            UUID createdByUserId) {}

    public record EditRequest(
            String processorName,
            String processorLegalEntity,
            String processorContact,
            String processorDpoContact,
            String processorCountry,
            String servicesDescription,
            Set<String> subProcessorCategories,
            Set<UUID> linkedProcessingActivityIds,
            Set<String> thirdCountryTransfers,
            String transferSafeguards,
            String contractDocumentUrl,
            Instant signedAt,
            Instant effectiveFrom,
            Instant expirationDate,
            String securityMeasures,
            int breachNotificationCommitmentHours,
            boolean auditRights,
            String auditRightsNotes,
            String dataReturnOrDeletionTerms) {}

    public record TerminateRequest(String reason) {}

    public record View(
            UUID id, UUID tenantId, String reference,
            String processorName, String processorLegalEntity,
            String processorContact, String processorDpoContact,
            String processorCountry, String servicesDescription,
            Set<String> subProcessorCategories,
            Set<UUID> linkedProcessingActivityIds,
            Set<String> thirdCountryTransfers, String transferSafeguards,
            String contractDocumentUrl,
            Instant signedAt, Instant effectiveFrom, Instant expirationDate,
            String securityMeasures, int breachNotificationCommitmentHours,
            boolean auditRights, String auditRightsNotes,
            String dataReturnOrDeletionTerms,
            ProcessorAgreementStatus status,
            Instant terminatedAt, String terminationReason,
            UUID createdByUserId, Instant createdAt, Instant updatedAt,
            boolean expirable
    ) {
        public static View of(ProcessorAgreement a, Instant now) {
            return new View(
                    a.getId(), a.getTenantId(), a.getReference(),
                    a.getProcessorName(), a.getProcessorLegalEntity(),
                    a.getProcessorContact(), a.getProcessorDpoContact(),
                    a.getProcessorCountry(), a.getServicesDescription(),
                    a.getSubProcessorCategories(),
                    a.getLinkedProcessingActivityIds(),
                    a.getThirdCountryTransfers(), a.getTransferSafeguards(),
                    a.getContractDocumentUrl(),
                    a.getSignedAt(), a.getEffectiveFrom(), a.getExpirationDate(),
                    a.getSecurityMeasures(), a.getBreachNotificationCommitmentHours(),
                    a.isAuditRights(), a.getAuditRightsNotes(),
                    a.getDataReturnOrDeletionTerms(),
                    a.getStatus(), a.getTerminatedAt(), a.getTerminationReason(),
                    a.getCreatedByUserId(), a.getCreatedAt(), a.getUpdatedAt(),
                    a.isExpirable(now));
        }
    }
}
