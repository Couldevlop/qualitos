package com.openlab.qualitos.quality.supplier;

import java.time.Instant;
import java.util.UUID;

public final class SupplierDocumentDto {

    private SupplierDocumentDto() {}

    /** Réponse de dépôt : sans URL de lecture, le client relit la liste. */
    public record Response(
            UUID id,
            UUID supplierId,
            SupplierDocumentOwnerType ownerType,
            UUID ownerId,
            SupplierDocumentCategory category,
            String contentType,
            long sizeBytes,
            String originalFilename,
            UUID uploadedBy,
            Instant createdAt
    ) {}

    /** Élément de liste : porte une URL présignée de courte durée, jamais la clé d'objet. */
    public record ListItem(
            UUID id,
            UUID supplierId,
            SupplierDocumentOwnerType ownerType,
            UUID ownerId,
            SupplierDocumentCategory category,
            String contentType,
            long sizeBytes,
            String originalFilename,
            UUID uploadedBy,
            Instant createdAt,
            String url
    ) {}

    /**
     * Ligne de la vue « Documents fournisseurs » de la GED : le document, son
     * fournisseur et son porteur nommés (référence de pièce de la NC, norme du
     * certificat), et une URL présignée de courte durée.
     */
    public record CatalogItem(
            UUID id,
            UUID supplierId,
            String supplierCode,
            String supplierName,
            SupplierDocumentOwnerType ownerType,
            UUID ownerId,
            String ownerLabel,
            SupplierDocumentCategory category,
            String contentType,
            long sizeBytes,
            String originalFilename,
            UUID uploadedBy,
            Instant createdAt,
            String url
    ) {}
}
