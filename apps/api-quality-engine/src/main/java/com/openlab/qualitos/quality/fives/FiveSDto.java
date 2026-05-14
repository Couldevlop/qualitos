package com.openlab.qualitos.quality.fives;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class FiveSDto {

    private FiveSDto() {}

    public record CreateAuditRequest(
            @NotBlank @Size(max = 200) String zone,
            String description,
            @NotNull UUID auditorId,
            Instant scheduledAt
    ) {}

    public record UpdateAuditRequest(
            @Size(max = 200) String zone,
            String description,
            Instant scheduledAt
    ) {}

    public record ScoreRequest(
            @NotNull FiveSPillar pillar,
            @NotNull @Min(0) @Max(10) Integer score,
            String note,
            @Size(max = 1024) String photoUrl
    ) {}

    public record AuditResponse(
            UUID id,
            UUID tenantId,
            String zone,
            String description,
            FiveSAuditStatus status,
            UUID auditorId,
            Instant scheduledAt,
            Instant completedAt,
            Double overallScore,
            Instant createdAt,
            Instant updatedAt,
            List<ItemResponse> items
    ) {}

    public record ItemResponse(
            UUID id,
            UUID auditId,
            FiveSPillar pillar,
            Integer score,
            String note,
            String photoUrl,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
