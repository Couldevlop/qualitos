package com.openlab.qualitos.quality.circle;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CircleDto {

    private CircleDto() {}

    public record CreateCircleRequest(
            @NotBlank @Size(max = 255) String name,
            String description,
            @Size(max = 255) String topic
    ) {}

    public record UpdateCircleRequest(
            @Size(max = 255) String name,
            String description,
            @Size(max = 255) String topic
    ) {}

    public record AddMemberRequest(
            @NotNull UUID userId,
            CircleRole role
    ) {}

    public record UpdateMemberRoleRequest(@NotNull CircleRole role) {}

    public record MeetingRequest(
            @NotBlank @Size(max = 255) String title,
            String agenda,
            @NotNull Instant scheduledAt,
            @Min(1) Integer durationMinutes,
            @Size(max = 500) String location
    ) {}

    public record UpdateMeetingRequest(
            @Size(max = 255) String title,
            String agenda,
            Instant scheduledAt,
            @Min(1) Integer durationMinutes,
            @Size(max = 500) String location
    ) {}

    public record HoldMeetingRequest(String minutes) {}

    public record ProposalRequest(
            @NotBlank @Size(max = 255) String title,
            String description,
            @NotNull UUID proposedBy,
            UUID meetingId
    ) {}

    public record ApproveProposalRequest(@NotNull UUID validatedBy) {}

    public record RejectProposalRequest(
            @NotNull UUID validatedBy,
            @NotBlank String reason
    ) {}

    public record ImpactRequest(@NotBlank String impactNote) {}

    /** Requête pour générer un compte-rendu LLM à partir d'un transcript (ANO-010). */
    public record GenerateMinutesRequest(@NotBlank String transcript) {}

    /** Action extraite par le LLM depuis le transcript de réunion. */
    public record ExtractedAction(String label, String suggestedAssignee) {}

    /** Compte-rendu structuré généré par LLM (§3.3 QualitOS). */
    public record MeetingMinutes(
            String summary,
            List<String> decisions,
            List<ExtractedAction> actions
    ) {}

    public record CircleResponse(
            UUID id,
            UUID tenantId,
            String name,
            String description,
            String topic,
            CircleStatus status,
            int memberCount,
            Instant createdAt,
            Instant updatedAt,
            List<MemberResponse> members,
            List<MeetingResponse> meetings,
            List<ProposalResponse> proposals
    ) {}

    public record MemberResponse(
            UUID id, UUID circleId, UUID userId, CircleRole role, Instant joinedAt) {}

    public record MeetingResponse(
            UUID id, UUID circleId, String title, String agenda,
            Instant scheduledAt, Integer durationMinutes, String location,
            MeetingStatus status, String minutes, Instant heldAt,
            Instant createdAt, Instant updatedAt,
            String minutesSummary, String minutesJson) {}

    public record ProposalResponse(
            UUID id, UUID circleId, UUID meetingId, String title, String description,
            ProposalStatus status, UUID proposedBy, UUID validatedBy,
            Instant validatedAt, Instant implementedAt, Instant measuredAt,
            String impactNote, String rejectionReason,
            Instant createdAt, Instant updatedAt) {}
}
