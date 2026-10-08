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

    /**
     * {@code authorId} n'est plus cru : l'auteur est l'utilisateur du jeton
     * (ADR 0080). Un identifiant différent est refusé (403) ; absent, il est déduit.
     */
    public record CreateVersionRequest(
            String content,
            String contentUri,
            String changeNote,
            UUID authorId
    ) {}

    public record UpdateVersionRequest(
            String content,
            String contentUri,
            String changeNote
    ) {}

    /** L'approbateur est l'utilisateur du jeton (ADR 0080) ; le champ n'est gardé que pour vérifier qu'il concorde. */
    public record ApprovalRequest(UUID approverId) {}

    /** Celui qui acquitte est l'utilisateur du jeton (ADR 0080) : nul n'acquitte pour un autre. */
    public record AcknowledgeRequest(UUID userId) {}

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
