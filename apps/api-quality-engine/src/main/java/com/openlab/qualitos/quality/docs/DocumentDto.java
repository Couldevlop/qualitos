package com.openlab.qualitos.quality.docs;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class DocumentDto {

    private DocumentDto() {}

    public record CreateDocumentRequest(
            @NotBlank @Size(max = 100) String code,
            @NotBlank @Size(max = 255) String title,
            String description,
            @NotNull DocumentType type,
            @NotNull UUID ownerId,
            boolean mandatoryRead,
            /** Contenu de la première version DRAFT (optionnel). */
            String initialContent,
            String initialContentUri,
            String initialChangeNote
    ) {}

    public record UpdateDocumentRequest(
            @Size(max = 255) String title,
            String description,
            DocumentType type,
            UUID ownerId,
            Boolean mandatoryRead
    ) {}

    public record CreateVersionRequest(
            String content,
            String contentUri,
            String changeNote,
            @NotNull UUID authorId
    ) {}

    public record UpdateVersionRequest(
            String content,
            String contentUri,
            String changeNote
    ) {}

    public record ApprovalRequest(@NotNull UUID approverId) {}

    public record AcknowledgeRequest(@NotNull UUID userId) {}

    public record DocumentResponse(
            UUID id,
            UUID tenantId,
            String code,
            String title,
            String description,
            DocumentType type,
            DocumentStatus status,
            UUID ownerId,
            UUID currentVersionId,
            boolean mandatoryRead,
            Instant createdAt,
            Instant updatedAt,
            List<VersionResponse> versions
    ) {}

    public record VersionResponse(
            UUID id,
            UUID documentId,
            Integer versionNumber,
            String content,
            String contentUri,
            String contentHash,
            String changeNote,
            VersionStatus status,
            UUID authorId,
            UUID approvedBy,
            Instant approvedAt,
            Instant publishedAt,
            String blockchainTxHash,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record AcknowledgmentResponse(
            UUID id,
            UUID versionId,
            UUID userId,
            Instant acknowledgedAt
    ) {}
}
