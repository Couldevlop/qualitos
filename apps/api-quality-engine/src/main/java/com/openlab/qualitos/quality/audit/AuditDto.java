package com.openlab.qualitos.quality.audit;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class AuditDto {

    private AuditDto() {}

    public record CreatePlanRequest(
            @NotBlank @Size(max = 255) String title,
            String scope,
            @NotNull AuditType type,
            @Size(max = 100) String standard,
            @NotNull UUID leadAuditorId,
            UUID auditeeId,
            LocalDate scheduledDate
    ) {}

    public record UpdatePlanRequest(
            @Size(max = 255) String title,
            String scope,
            AuditType type,
            @Size(max = 100) String standard,
            UUID leadAuditorId,
            UUID auditeeId,
            LocalDate scheduledDate
    ) {}

    public record CompleteRequest(String reportSummary) {}

    public record ChecklistItemRequest(
            @NotBlank String question,
            @Size(max = 100) String clauseRef,
            String expectedEvidence,
            @Min(1) Integer weight,
            @Min(0) Integer orderIndex
    ) {}

    public record ChecklistResponseRequest(
            String response,
            Boolean conformant
    ) {}

    public record FindingRequest(
            @NotNull FindingType type,
            @NotBlank String description,
            @Size(max = 100) String clauseRef,
            @Size(max = 1024) String photoUrl,
            UUID checklistItemId,
            UUID capaId,
            @NotNull UUID raisedBy
    ) {}

    public record UpdateFindingRequest(
            FindingType type,
            String description,
            @Size(max = 100) String clauseRef,
            @Size(max = 1024) String photoUrl,
            UUID capaId
    ) {}

    public record PlanResponse(
            UUID id,
            UUID tenantId,
            String title,
            String scope,
            AuditType type,
            AuditStatus status,
            String standard,
            UUID leadAuditorId,
            UUID auditeeId,
            LocalDate scheduledDate,
            Instant startedAt,
            Instant completedAt,
            String reportSummary,
            Instant createdAt,
            Instant updatedAt,
            List<ChecklistItemResponse> checklist,
            List<FindingResponse> findings,
            Double conformityScore
    ) {}

    public record ChecklistItemResponse(
            UUID id,
            UUID planId,
            String question,
            String clauseRef,
            String expectedEvidence,
            Integer weight,
            Integer orderIndex,
            String response,
            Boolean conformant,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record FindingResponse(
            UUID id,
            UUID planId,
            UUID checklistItemId,
            FindingType type,
            String description,
            String clauseRef,
            String photoUrl,
            UUID capaId,
            UUID raisedBy,
            Instant raisedAt,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
