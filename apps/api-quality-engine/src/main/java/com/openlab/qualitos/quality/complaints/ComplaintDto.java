package com.openlab.qualitos.quality.complaints;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class ComplaintDto {

    private ComplaintDto() {}

    public record CreateComplaintRequest(
            @NotBlank @Size(max = 100)
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._\\-]{1,99}$",
                    message = "code must be alphanumeric kebab/snake (2..100 chars)")
            String code,
            @NotNull ComplaintChannel channel,
            @Size(max = 250) String customerName,
            @Email @Size(max = 320) String customerEmail,
            @Size(max = 200) String customerExternalId,
            @NotBlank @Size(max = 250) String subject,
            @Size(max = 4000) String description,
            ComplaintSeverity severity,
            ComplaintCategory category,
            UUID supplierId,
            UUID assignedToUserId,
            @NotNull UUID createdBy,
            Instant receivedAt
    ) {}

    public record UpdateComplaintRequest(
            @Size(max = 250) String customerName,
            @Email @Size(max = 320) String customerEmail,
            @Size(max = 200) String customerExternalId,
            @Size(max = 250) String subject,
            @Size(max = 4000) String description,
            ComplaintSeverity severity,
            ComplaintCategory category,
            UUID supplierId,
            UUID assignedToUserId
    ) {}

    public record ComplaintResponse(
            UUID id, UUID tenantId, String code,
            ComplaintChannel channel,
            String customerName, String customerEmail, String customerExternalId,
            String subject, String description,
            ComplaintSeverity severity, ComplaintCategory category,
            ComplaintStatus status,
            UUID supplierId, UUID capaCaseId, UUID assignedToUserId,
            Integer satisfactionScore,
            Instant receivedAt, Instant firstResponseAt,
            Instant resolvedAt, Instant closedAt,
            String rejectionReason,
            UUID createdBy, Instant createdAt, Instant updatedAt
    ) {}

    public record AssignRequest(
            @NotNull UUID assigneeUserId
    ) {}

    public record AddResponseRequest(
            @NotNull UUID authorUserId,
            ComplaintChannel channel,
            @NotBlank @Size(max = 4000) String body,
            boolean internalNote
    ) {}

    public record ResponseEntryResponse(
            UUID id, UUID tenantId, UUID complaintId,
            UUID authorUserId, ComplaintChannel channel,
            String body, boolean internalNote,
            Instant sentAt, Instant createdAt
    ) {}

    public record RejectRequest(
            @NotBlank @Size(max = 1000) String reason
    ) {}

    public record ResolveRequest(
            UUID capaCaseId
    ) {}

    public record SatisfactionRequest(
            @Min(0) @Max(10) @NotNull Integer score
    ) {}

    public record ComplaintStatistics(
            UUID tenantId,
            long total,
            long received,
            long underInvestigation,
            long responded,
            long resolved,
            long closed,
            long rejected,
            long product,
            long service,
            long delivery,
            long billing,
            long quality,
            long safety,
            long other
    ) {}
}
