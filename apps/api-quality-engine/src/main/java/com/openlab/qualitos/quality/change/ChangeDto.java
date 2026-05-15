package com.openlab.qualitos.quality.change;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ChangeDto {

    private ChangeDto() {}

    public record CreateChangeRequest(
            @NotBlank @Size(max = 100)
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._\\-]{1,99}$",
                    message = "code must be alphanumeric kebab/snake (2..100 chars)")
            String code,
            @NotBlank @Size(max = 250) String title,
            @Size(max = 4000) String description,
            @NotNull ChangeRequestType type,
            ChangeRequestPriority priority,
            @NotNull UUID requesterUserId,
            UUID ownerUserId,
            LocalDate plannedFor,
            @Size(max = 2000) String impactSummary,
            @Size(max = 2000) String riskAssessment
    ) {}

    public record UpdateChangeRequest(
            @Size(max = 250) String title,
            @Size(max = 4000) String description,
            ChangeRequestPriority priority,
            UUID ownerUserId,
            LocalDate plannedFor,
            @Size(max = 2000) String impactSummary,
            @Size(max = 2000) String riskAssessment
    ) {}

    public record ChangeResponse(
            UUID id, UUID tenantId, String code, String title, String description,
            ChangeRequestType type, ChangeRequestPriority priority, ChangeRequestStatus status,
            UUID requesterUserId, UUID ownerUserId,
            LocalDate plannedFor, LocalDate implementedAt,
            String impactSummary, String riskAssessment, String rejectionReason,
            Instant createdAt, Instant updatedAt
    ) {}

    public record AddImpactRequest(
            @NotNull ChangeImpactTargetType targetType,
            @NotNull UUID targetId,
            @Size(max = 1000) String notes
    ) {}

    public record ImpactResponse(
            UUID id, UUID tenantId, UUID changeId,
            ChangeImpactTargetType targetType, UUID targetId,
            String notes, Instant createdAt
    ) {}

    public record AddApproverRequest(
            @NotNull UUID approverUserId,
            @Min(1) Integer approvalLevel
    ) {}

    public record DecisionRequest(
            @NotNull UUID approverUserId,
            @NotNull ApprovalDecision decision,
            @Size(max = 1000) String comment
    ) {}

    public record ApprovalResponse(
            UUID id, UUID tenantId, UUID changeId, UUID approverUserId,
            int approvalLevel, ApprovalDecision decision, String comment,
            Instant decidedAt, Instant createdAt
    ) {}

    public record ImplementRequest(
            @NotNull LocalDate implementedAt
    ) {}

    public record ChangeSummary(
            UUID changeId,
            ChangeRequestStatus status,
            long totalApprovers,
            long approved,
            long rejected,
            long pending,
            int impactCount,
            List<ApprovalResponse> approvals,
            List<ImpactResponse> impacts
    ) {}
}
