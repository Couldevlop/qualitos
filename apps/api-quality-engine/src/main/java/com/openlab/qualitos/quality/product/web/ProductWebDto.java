package com.openlab.qualitos.quality.product.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Requêtes HTTP du référentiel Produit. Aucune ne porte de {@code tenantId} : le
 * tenant vient toujours du JWT côté service (règle 18.2 #2). Les bornes Jakarta
 * reprennent les maxima des colonnes DB (migration V110) : sans elles, une valeur
 * trop longue franchirait la validation et ressortirait en exception de
 * persistance brute au lieu d'un 400 propre à la frontière.
 */
public final class ProductWebDto {

    private ProductWebDto() {}

    public record CreateRequest(
            @NotBlank @Size(max = 64)
            @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$",
                    message = "code must match [A-Za-z0-9][A-Za-z0-9._-]{0,63}")
            String code,
            @NotBlank @Size(max = 250) String designation,
            @Size(max = 120) String family,
            @Size(max = 16) String revisionIndex,
            @Size(max = 250) String customerLabel,
            @Size(max = 250) String siteLabel,
            UUID ownerUserId) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 250) String designation,
            @Size(max = 120) String family,
            @Size(max = 16) String revisionIndex,
            @Size(max = 250) String customerLabel,
            @Size(max = 250) String siteLabel,
            UUID ownerUserId) {}

    public record ComponentRequest(
            @PositiveOrZero int sequenceNo,
            String reference,
            @Size(max = 250) String label,
            BigDecimal quantity,
            @Size(max = 24) String unit,
            UUID supplierId) {}

    public record OperationRequest(
            @PositiveOrZero int sequenceNo,
            String code,
            String label,
            String workstation) {}
}
