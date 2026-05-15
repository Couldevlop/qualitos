package com.openlab.qualitos.quality.risk;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class FmeaDto {

    private FmeaDto() {}

    public record CreateProjectRequest(
            @NotBlank @Size(max = 120)
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._\\-]{0,119}$",
                    message = "code must be alphanumeric kebab/snake (max 120 chars)")
            String code,
            @NotBlank @Size(max = 250) String name,
            @Size(max = 1000) String scope,
            @NotNull FmeaType type,
            @Min(1) @Max(1000) Integer criticalRpnThreshold,
            UUID ownerUserId,
            @NotNull UUID createdBy
    ) {}

    public record UpdateProjectRequest(
            @Size(max = 250) String name,
            @Size(max = 1000) String scope,
            @Min(1) @Max(1000) Integer criticalRpnThreshold,
            UUID ownerUserId
    ) {}

    public record ProjectResponse(
            UUID id,
            UUID tenantId,
            String code,
            String name,
            String scope,
            FmeaType type,
            FmeaStatus status,
            int criticalRpnThreshold,
            int revision,
            UUID ownerUserId,
            Instant lastReviewedAt,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record CreateItemRequest(
            @Size(max = 500)  String function,
            @Size(max = 500)  String failureMode,
            @Size(max = 500)  String failureEffect,
            @Size(max = 1000) String failureCause,
            @Size(max = 1000) String currentControls,
            @Min(1) @Max(10) @NotNull Integer severity,
            @Min(1) @Max(10) @NotNull Integer occurrence,
            @Min(1) @Max(10) @NotNull Integer detection,
            @Size(max = 1000) String recommendedAction,
            UUID actionOwnerUserId,
            LocalDate actionDueDate,
            @Min(1) @Max(10) Integer resultingSeverity,
            @Min(1) @Max(10) Integer resultingOccurrence,
            @Min(1) @Max(10) Integer resultingDetection
    ) {}

    public record UpdateItemRequest(
            @Size(max = 500)  String function,
            @Size(max = 500)  String failureMode,
            @Size(max = 500)  String failureEffect,
            @Size(max = 1000) String failureCause,
            @Size(max = 1000) String currentControls,
            @Min(1) @Max(10) Integer severity,
            @Min(1) @Max(10) Integer occurrence,
            @Min(1) @Max(10) Integer detection,
            @Size(max = 1000) String recommendedAction,
            UUID actionOwnerUserId,
            LocalDate actionDueDate,
            @Min(1) @Max(10) Integer resultingSeverity,
            @Min(1) @Max(10) Integer resultingOccurrence,
            @Min(1) @Max(10) Integer resultingDetection
    ) {}

    public record ItemResponse(
            UUID id,
            UUID tenantId,
            UUID projectId,
            int sequenceNo,
            String function,
            String failureMode,
            String failureEffect,
            String failureCause,
            String currentControls,
            int severity,
            int occurrence,
            int detection,
            int rpn,
            String recommendedAction,
            UUID actionOwnerUserId,
            LocalDate actionDueDate,
            Integer resultingSeverity,
            Integer resultingOccurrence,
            Integer resultingDetection,
            Integer rpnAfter,
            boolean critical,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record ProjectStatistics(
            UUID projectId,
            long totalItems,
            long criticalItems,
            int maxRpn,
            double averageRpn,
            int criticalRpnThreshold
    ) {}
}
