package com.openlab.qualitos.quality.itsm;

import java.time.Instant;

/**
 * DTO neutre représentant un incident lu depuis un ITSM externe.
 * Chaque {@link ItsmProviderClient} traduit son format propre vers cette structure
 * avant que le service domaine ne décide quoi en faire.
 */
public record ExternalIncident(
        String externalId,
        String title,
        String description,
        String status,
        String priority,
        Instant createdAt,
        Instant updatedAt,
        String url
) {}
