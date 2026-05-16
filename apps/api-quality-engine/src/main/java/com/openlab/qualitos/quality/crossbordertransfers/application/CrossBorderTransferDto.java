package com.openlab.qualitos.quality.crossbordertransfers.application;

import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransfer;
import com.openlab.qualitos.quality.crossbordertransfers.domain.CrossBorderTransferStatus;
import com.openlab.qualitos.quality.crossbordertransfers.domain.TransferMechanism;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class CrossBorderTransferDto {

    private CrossBorderTransferDto() {}

    public record CreateRequest(
            String reference,
            String recipientName,
            String recipientLegalEntity,
            String recipientContact,
            Set<String> destinationCountries,
            TransferMechanism mechanism,
            String safeguardsDescription,
            String safeguardsDocumentUrl,
            String derogationJustification,
            Set<String> dataCategories,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedProcessorAgreementIds,
            UUID createdByUserId) {}

    public record EditRequest(
            String recipientName,
            String recipientLegalEntity,
            String recipientContact,
            Set<String> destinationCountries,
            TransferMechanism mechanism,
            String safeguardsDescription,
            String safeguardsDocumentUrl,
            String derogationJustification,
            Set<String> dataCategories,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedProcessorAgreementIds) {}

    public record SuspendRequest(String reason) {}
    public record TerminateRequest(String reason) {}

    public record View(
            UUID id, UUID tenantId, String reference,
            String recipientName, String recipientLegalEntity, String recipientContact,
            Set<String> destinationCountries,
            TransferMechanism mechanism,
            String safeguardsDescription, String safeguardsDocumentUrl,
            String derogationJustification,
            Set<String> dataCategories,
            Set<UUID> linkedProcessingActivityIds,
            Set<UUID> linkedProcessorAgreementIds,
            CrossBorderTransferStatus status,
            Instant effectiveFrom, Instant effectiveTo,
            Instant suspendedAt, String suspensionReason, String terminationReason,
            UUID createdByUserId, Instant createdAt, Instant updatedAt
    ) {
        public static View of(CrossBorderTransfer t) {
            return new View(
                    t.getId(), t.getTenantId(), t.getReference(),
                    t.getRecipientName(), t.getRecipientLegalEntity(), t.getRecipientContact(),
                    t.getDestinationCountries(),
                    t.getMechanism(),
                    t.getSafeguardsDescription(), t.getSafeguardsDocumentUrl(),
                    t.getDerogationJustification(),
                    t.getDataCategories(),
                    t.getLinkedProcessingActivityIds(), t.getLinkedProcessorAgreementIds(),
                    t.getStatus(),
                    t.getEffectiveFrom(), t.getEffectiveTo(),
                    t.getSuspendedAt(), t.getSuspensionReason(), t.getTerminationReason(),
                    t.getCreatedByUserId(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }
}
