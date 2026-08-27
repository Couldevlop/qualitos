package com.openlab.qualitos.quality.pdca;

import java.time.Instant;
import java.util.UUID;

public final class PdcaStepEvidenceDto {

    private PdcaStepEvidenceDto() {}

    /** Réponse de dépôt (POST) : sans URL présignée, le client relit par le GET. */
    public record Response(
            UUID id,
            UUID cycleId,
            UUID stepId,
            String contentType,
            long sizeBytes,
            String originalFilename,
            UUID uploadedBy,
            Instant createdAt
    ) {}

    /**
     * Élément de liste (GET) : porte une URL de lecture à durée de vie courte.
     *
     * <p>L'URL est présignée à chaque lecture plutôt que stockée : une adresse
     * de téléchargement qui vivrait dans la base finirait par circuler bien
     * au-delà du tenant qui l'a obtenue.
     */
    public record ListItem(
            UUID id,
            UUID cycleId,
            UUID stepId,
            String contentType,
            long sizeBytes,
            String originalFilename,
            UUID uploadedBy,
            Instant createdAt,
            String url
    ) {}
}
