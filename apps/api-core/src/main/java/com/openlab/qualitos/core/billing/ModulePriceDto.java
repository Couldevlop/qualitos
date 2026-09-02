package com.openlab.qualitos.core.billing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO du tarif d'un module. Records Java 21, comme
 * {@link BillingProfileDto} : la commande d'écriture ({@code SaveCommand})
 * ne connaît ni l'id, ni l'acteur, ni l'horodatage — {@link ModulePriceService}
 * seul les attribue, l'acteur venant du jeton d'authentification et non du
 * corps de la requête (voir {@link ModulePriceController}).
 */
public sealed interface ModulePriceDto permits ModulePriceDto.SaveCommand, ModulePriceDto.View {

    /**
     * Commande d'écriture (création ou mise à jour d'un tarif). Les
     * validations annotées miroitent les contraintes SQL de
     * {@code module_prices} : {@code amountCents} ne peut pas être négatif
     * (un prix nul reste légitime pour le palier FREE), {@code currency}
     * doit être un code ISO 4217.
     */
    record SaveCommand(
        @NotBlank
        @Size(max = 64)
        String moduleCode,

        @NotNull
        BillingTier billingTier,

        @NotNull
        BillingPeriod period,

        @PositiveOrZero
        long amountCents,

        @NotBlank
        @Pattern(regexp = "^[A-Z]{3}$")
        String currency
    ) implements ModulePriceDto {}

    record View(
        UUID id,
        String moduleCode,
        BillingTier billingTier,
        BillingPeriod period,
        long amountCents,
        String currency,
        UUID updatedBy,
        Instant updatedAt
    ) implements ModulePriceDto {

        public static View from(ModulePrice price) {
            return new View(
                price.getId(),
                price.getModuleCode(),
                price.getBillingTier(),
                price.getPeriod(),
                price.getAmountCents(),
                price.getCurrency(),
                price.getUpdatedBy(),
                price.getUpdatedAt()
            );
        }
    }
}
