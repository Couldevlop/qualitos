package com.openlab.qualitos.quality.apqp;

import java.time.Instant;
import java.util.UUID;

public final class ApqpDeliverableEvidenceDto {

    private ApqpDeliverableEvidenceDto() {}

    /** Réponse de dépôt : sans URL présignée, l'écran relit par le GET. */
    public record Response(
            UUID id,
            UUID phaseId,
            UUID deliverableId,
            String contentType,
            long sizeBytes,
            String originalFilename,
            UUID uploadedBy,
            Instant createdAt,
            String objectKey) {}

    /**
     * Élément de liste : porte une URL de lecture à durée de vie courte.
     *
     * <p>L'URL est présignée à chaque lecture plutôt que stockée : une adresse de
     * téléchargement qui vivrait dans la base finirait par circuler bien au-delà
     * du client qui l'a obtenue.
     */
    public record ListItem(
            UUID id,
            UUID phaseId,
            UUID deliverableId,
            String contentType,
            long sizeBytes,
            String originalFilename,
            UUID uploadedBy,
            Instant createdAt,
            String url) {}
}
