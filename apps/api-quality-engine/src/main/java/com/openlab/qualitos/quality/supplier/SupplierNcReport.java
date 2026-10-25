package com.openlab.qualitos.quality.supplier;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Ce que dit la fiche imprimée d'une NC fournisseur : déjà mis en mots, pour que
 * le moteur de mise en page n'ait rien à interpréter.
 */
public record SupplierNcReport(
        String supplierCode,
        String supplierName,
        String partNumber,
        Integer partQuantity,
        String lotReference,
        LocalDate detectedOn,
        LocalDate resolvedOn,
        NonConformitySeverity severity,
        NonConformityStatus status,
        boolean eightDRequested,
        boolean eightDReceived,
        String description,
        String resolution,
        List<Attachment> attachments,
        Instant editedAt
) {

    public SupplierNcReport {
        attachments = attachments == null ? List.of() : List.copyOf(attachments);
    }

    /** Une pièce jointe telle que la fiche la cite : sa nature, son nom, son poids, sa date. */
    public record Attachment(SupplierDocumentCategory category, String filename, long sizeBytes, Instant addedAt) {}
}
