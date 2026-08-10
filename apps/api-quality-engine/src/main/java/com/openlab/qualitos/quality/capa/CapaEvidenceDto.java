package com.openlab.qualitos.quality.capa;

import java.time.Instant;
import java.util.UUID;

public final class CapaEvidenceDto {

    private CapaEvidenceDto() {}

    /** Réponse de dépôt (POST) : sans URL présignée, le client relit par le GET. */
    public record Response(
            UUID id,
            UUID capaId,
            // Action visée, ou null quand la pièce vaut pour le dossier entier.
            // Le champ est TOUJOURS présent dans la charge utile : un client qui
            // range les preuves par action doit pouvoir distinguer « pièce du
            // dossier » d'une action qu'il ne connaîtrait pas encore.
            UUID actionId,
            String contentType,
            long sizeBytes,
            String originalFilename,
            UUID uploadedBy,
            Instant createdAt
    ) {}

    /** Élément de liste (GET) : porte une URL de lecture à durée de vie courte. */
    public record ListItem(
            UUID id,
            UUID capaId,
            UUID actionId,
            String contentType,
            long sizeBytes,
            String originalFilename,
            UUID uploadedBy,
            Instant createdAt,
            String url
    ) {}
}
