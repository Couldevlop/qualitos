package com.openlab.qualitos.core.billing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de l'abonnement. Records Java 21, comme {@link BillingProfileDto} et
 * {@link ModulePriceDto}.
 *
 * <p>{@code SubscribeCommand} ne porte NI le client, NI l'acteur, NI le prix :
 * le client vient du chemin (§18.2 règle 2), l'acteur du jeton (§18.2 règle 5),
 * le prix du catalogue (voir {@link SubscriptionService#subscribe}). Ce qui
 * n'est pas dans la commande ne peut pas être forgé par l'appelant — trois
 * champs absents valent mieux que trois validations à ne pas oublier.
 */
public sealed interface SubscriptionDto permits SubscriptionDto.SubscribeCommand, SubscriptionDto.View {

    /**
     * Commande de souscription : quel module, à quel palier, dans quelle
     * périodicité. Le triplet sert de clé au catalogue tarifaire
     * ({@code uk_module_price}) : c'est exactement ce qu'il faut pour connaître
     * le prix, et rien de plus.
     */
    record SubscribeCommand(
        @NotBlank
        @Size(max = 64)
        String moduleCode,

        @NotNull
        BillingTier billingTier,

        @NotNull
        BillingPeriod period
    ) implements SubscriptionDto {}

    record View(
        UUID id,
        UUID tenantId,
        String moduleCode,
        BillingTier billingTier,
        BillingPeriod period,
        long amountCents,
        String currency,
        LocalDate startedOn,
        LocalDate nextRenewal,
        Instant cancelledAt,
        UUID cancelledBy,
        Instant createdAt,
        UUID createdBy
    ) implements SubscriptionDto {

        public static View from(Subscription subscription) {
            return new View(
                subscription.getId(),
                subscription.getTenantId(),
                subscription.getModuleCode(),
                subscription.getBillingTier(),
                subscription.getPeriod(),
                subscription.getAmountCents(),
                subscription.getCurrency(),
                subscription.getStartedOn(),
                subscription.getNextRenewal(),
                subscription.getCancelledAt(),
                subscription.getCancelledBy(),
                subscription.getCreatedAt(),
                subscription.getCreatedBy()
            );
        }

        /** Le montant convenu, devise comprise — jamais un {@code long} nu. */
        public Money amount() {
            return Money.of(amountCents, currency);
        }
    }
}
