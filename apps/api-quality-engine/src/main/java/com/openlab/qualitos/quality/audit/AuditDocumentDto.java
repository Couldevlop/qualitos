package com.openlab.qualitos.quality.audit;

import java.time.Instant;
import java.util.UUID;

public final class AuditDocumentDto {

    private AuditDocumentDto() {}

    /** Réponse de dépôt : sans URL de lecture, le client relit la liste. */
    public record Response(
            UUID id,
            UUID planId,
            String contentType,
            long sizeBytes,
            String originalFilename,
            UUID uploadedBy,
            Instant createdAt
    ) {}

    /** Élément de liste : porte une URL présignée de courte durée, jamais la clé d'objet. */
    public record ListItem(
            UUID id,
            UUID planId,
            String contentType,
            long sizeBytes,
            String originalFilename,
            UUID uploadedBy,
            Instant createdAt,
            String url
    ) {}
}
