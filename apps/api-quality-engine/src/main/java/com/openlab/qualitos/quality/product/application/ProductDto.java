package com.openlab.qualitos.quality.product.application;

import com.openlab.qualitos.quality.product.domain.Product;
import com.openlab.qualitos.quality.product.domain.ProductComponent;
import com.openlab.qualitos.quality.product.domain.ProductOperation;
import com.openlab.qualitos.quality.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Commandes et vues du référentiel Produit. Forme de {@code DpoAppointmentDto} :
 * conteneur de {@code record}s, aucune logique. Les commandes ne portent jamais
 * de {@code tenantId} : le tenant vient toujours du contexte de sécurité côté
 * service, jamais d'une valeur que le client pourrait forger dans le corps.
 */
public final class ProductDto {

    private ProductDto() {}

    public record CreateCommand(
            String code, String designation, String family, String revisionIndex,
            String customerLabel, String siteLabel, UUID ownerUserId) {}

    public record UpdateCommand(
            String designation, String family, String revisionIndex,
            String customerLabel, String siteLabel, UUID ownerUserId) {}

    public record ComponentCommand(
            int sequenceNo, String reference, String label,
            BigDecimal quantity, String unit, UUID supplierId) {}

    public record OperationCommand(
            int sequenceNo, String code, String label, String workstation) {}

    public record View(
            UUID id, String code, String designation, String family, String revisionIndex,
            ProductStatus status, String customerLabel, String siteLabel, UUID ownerUserId,
            Instant createdAt, Instant updatedAt) {

        public static View of(Product p) {
            return new View(
                    p.getId(), p.getCode(), p.getDesignation(), p.getFamily(), p.getRevisionIndex(),
                    p.getStatus(), p.getCustomerLabel(), p.getSiteLabel(), p.getOwnerUserId(),
                    p.getCreatedAt(), p.getUpdatedAt());
        }
    }

    public record ComponentView(
            UUID id, int sequenceNo, String reference, String label,
            BigDecimal quantity, String unit, UUID supplierId) {

        public static ComponentView of(ProductComponent c) {
            return new ComponentView(
                    c.getId(), c.getSequenceNo(), c.getReference(), c.getLabel(),
                    c.getQuantity(), c.getUnit(), c.getSupplierId());
        }
    }

    public record OperationView(
            UUID id, int sequenceNo, String code, String label, String workstation) {

        public static OperationView of(ProductOperation o) {
            return new OperationView(
                    o.getId(), o.getSequenceNo(), o.getCode(), o.getLabel(), o.getWorkstation());
        }
    }
}
