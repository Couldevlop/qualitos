package com.openlab.qualitos.quality.aiactfria.application;

import com.openlab.qualitos.quality.aiactfria.domain.Fria;
import com.openlab.qualitos.quality.aiactfria.domain.FriaStatus;

import java.time.Instant;
import java.util.UUID;

public final class FriaDto {

    private FriaDto() {}

    public record DraftRequest(
            String reference, UUID aiSystemId,
            String processDescription, String deploymentDurationDescription,
            String affectedPersonsCategories, String specificRisks,
            String mitigationMeasures, String humanOversightMeasures,
            String complaintMechanismDescription,
            UUID createdByUserId) {}

    public record EditRequest(
            String processDescription, String deploymentDurationDescription,
            String affectedPersonsCategories, String specificRisks,
            String mitigationMeasures, String humanOversightMeasures,
            String complaintMechanismDescription) {}

    public record SubmitRequest(UUID submittedByUserId) {}

    public record ApproveRequest(UUID approvedByUserId, String approvalNotes) {}

    public record ReturnRequest(String reason) {}

    public record ArchiveRequest(String reason) {}

    public record View(
            UUID id, UUID tenantId, String reference, UUID aiSystemId,
            String processDescription, String deploymentDurationDescription,
            String affectedPersonsCategories, String specificRisks,
            String mitigationMeasures, String humanOversightMeasures,
            String complaintMechanismDescription,
            FriaStatus status,
            Instant submittedAt, UUID submittedByUserId,
            Instant approvedAt, UUID approvedByUserId, String approvalNotes,
            Instant effectiveTo, String archivedReason,
            UUID createdByUserId, Instant createdAt, Instant updatedAt
    ) {
        public static View of(Fria f) {
            return new View(
                    f.getId(), f.getTenantId(), f.getReference(), f.getAiSystemId(),
                    f.getProcessDescription(), f.getDeploymentDurationDescription(),
                    f.getAffectedPersonsCategories(), f.getSpecificRisks(),
                    f.getMitigationMeasures(), f.getHumanOversightMeasures(),
                    f.getComplaintMechanismDescription(),
                    f.getStatus(),
                    f.getSubmittedAt(), f.getSubmittedByUserId(),
                    f.getApprovedAt(), f.getApprovedByUserId(), f.getApprovalNotes(),
                    f.getEffectiveTo(), f.getArchivedReason(),
                    f.getCreatedByUserId(), f.getCreatedAt(), f.getUpdatedAt());
        }
    }
}
