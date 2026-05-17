package com.openlab.qualitos.quality.aiqms.application;

import com.openlab.qualitos.quality.aiqms.domain.AiQms;
import com.openlab.qualitos.quality.aiqms.domain.AiQmsStatus;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public final class AiQmsDto {

    private AiQmsDto() {}

    public record DraftRequest(
            String reference, String version, String name, String description,
            String regulatoryComplianceStrategy, String designControlDescription,
            String qualityControlDescription, String dataManagementDescription,
            String riskManagementDescription, String pmmDescription,
            String regulatorCommunicationDescription,
            String resourceManagementDescription, String supplierMonitoringDescription,
            Set<UUID> coveredAiSystemIds,
            UUID createdByUserId) {}

    public record EditRequest(
            String name, String description,
            String regulatoryComplianceStrategy, String designControlDescription,
            String qualityControlDescription, String dataManagementDescription,
            String riskManagementDescription, String pmmDescription,
            String regulatorCommunicationDescription,
            String resourceManagementDescription, String supplierMonitoringDescription,
            Set<UUID> coveredAiSystemIds) {}

    public record ApproveRequest(
            UUID submittedByUserId, UUID approvedByUserId, String approvalNotes) {}

    public record SupersedeRequest(UUID supersededByQmsId) {}

    public record ArchiveRequest(String reason) {}

    public record View(
            UUID id, UUID tenantId, String reference, String version,
            String name, String description,
            String regulatoryComplianceStrategy, String designControlDescription,
            String qualityControlDescription, String dataManagementDescription,
            String riskManagementDescription, String pmmDescription,
            String regulatorCommunicationDescription,
            String resourceManagementDescription, String supplierMonitoringDescription,
            Set<UUID> coveredAiSystemIds,
            AiQmsStatus status,
            Instant submittedAt, UUID submittedByUserId,
            Instant approvedAt, UUID approvedByUserId, String approvalNotes,
            Instant effectiveFrom, Instant effectiveTo,
            UUID supersededByQmsId, String archivedReason,
            UUID createdByUserId, Instant createdAt, Instant updatedAt
    ) {
        public static View of(AiQms q) {
            return new View(
                    q.getId(), q.getTenantId(), q.getReference(), q.getVersion(),
                    q.getName(), q.getDescription(),
                    q.getRegulatoryComplianceStrategy(), q.getDesignControlDescription(),
                    q.getQualityControlDescription(), q.getDataManagementDescription(),
                    q.getRiskManagementDescription(), q.getPmmDescription(),
                    q.getRegulatorCommunicationDescription(),
                    q.getResourceManagementDescription(), q.getSupplierMonitoringDescription(),
                    q.getCoveredAiSystemIds(),
                    q.getStatus(),
                    q.getSubmittedAt(), q.getSubmittedByUserId(),
                    q.getApprovedAt(), q.getApprovedByUserId(), q.getApprovalNotes(),
                    q.getEffectiveFrom(), q.getEffectiveTo(),
                    q.getSupersededByQmsId(), q.getArchivedReason(),
                    q.getCreatedByUserId(), q.getCreatedAt(), q.getUpdatedAt());
        }
    }
}
