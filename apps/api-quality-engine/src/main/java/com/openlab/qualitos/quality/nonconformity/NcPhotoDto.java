package com.openlab.qualitos.quality.nonconformity;

import java.time.Instant;
import java.util.UUID;

public final class NcPhotoDto {

    private NcPhotoDto() {}

    /** Réponse de création (POST) : pas d'URL présignée (le client relit via GET). */
    public record Response(
            UUID id,
            UUID ncId,
            String objectKey,
            String contentType,
            long sizeBytes,
            String originalFilename,
            Instant createdAt
    ) {}

    /** Élément de liste (GET) : inclut une URL présignée de lecture (TTL court). */
    public record ListItem(
            UUID id,
            UUID ncId,
            String objectKey,
            String contentType,
            long sizeBytes,
            String originalFilename,
            Instant createdAt,
            String url
    ) {}
}
