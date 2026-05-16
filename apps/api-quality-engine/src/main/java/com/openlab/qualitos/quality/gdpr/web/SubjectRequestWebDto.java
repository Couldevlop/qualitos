package com.openlab.qualitos.quality.gdpr.web;

import com.openlab.qualitos.quality.gdpr.domain.SubjectRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public final class SubjectRequestWebDto {

    private SubjectRequestWebDto() {}

    public record ReceiveRequest(
            @NotNull SubjectRequestType type,
            @NotBlank @Size(max = 320) String subjectIdentifier,
            @Size(max = 250) String subjectIdentifierLabel,
            @NotNull UUID requestedByUserId) {}

    public record StartProcessingRequest(@NotNull UUID handledByUserId) {}

    public record CompleteRequest(
            @NotBlank @Size(max = 4000) String resolutionNotes,
            @Size(max = 1024) String evidenceUrl,
            UUID handledByUserId) {}

    public record RejectRequest(
            @NotBlank @Size(max = 2000) String reason,
            UUID handledByUserId) {}

    public record ExtendDeadlineRequest(@NotNull Instant newDeadline) {}
}
