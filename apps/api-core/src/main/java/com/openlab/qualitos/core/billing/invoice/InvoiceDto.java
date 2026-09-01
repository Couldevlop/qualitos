package com.openlab.qualitos.core.billing.invoice;

import com.openlab.qualitos.core.billing.BillingPeriod;
import com.openlab.qualitos.core.billing.BillingTier;

import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * DTO de la facture. Records Java 21, comme les autres DTO de facturation.
 *
 * <p>Il n'y a pas de commande d'écriture : une facture ne se saisit pas, elle
 * s'ÉMET depuis les abonnements d'une période (voir
 * {@link InvoiceService#issueFor}). Accepter une facture rédigée par l'appelant
 * permettrait d'inventer des montants qu'aucun contrat ne justifie — c'est le
 * même raisonnement qui interdit de lire le client depuis le corps d'une
 * requête. Seule la PÉRIODE est fournie, par paramètre.
 */
public sealed interface InvoiceDto permits InvoiceDto.View, InvoiceDto.LineView {

    record LineView(
        int lineNo,
        UUID subscriptionId,
        String moduleCode,
        BillingTier billingTier,
        BillingPeriod period,
        int quantity,
        long unitAmountCents,
        long lineTotalCents
    ) implements InvoiceDto {

        public static LineView from(InvoiceLine line) {
            return new LineView(
                line.getLineNo(),
                line.getSubscriptionId(),
                line.getModuleCode(),
                line.getBillingTier(),
                line.getPeriod(),
                line.getQuantity(),
                line.getUnitAmountCents(),
                line.getLineTotalCents()
            );
        }
    }

    record View(
        UUID id,
        UUID tenantId,
        String number,
        int fiscalYear,
        YearMonth period,
        String currency,
        long totalCents,
        Instant issuedAt,
        UUID issuedBy,
        Instant sentAt,
        String sentTo,
        List<LineView> lines
    ) implements InvoiceDto {

        public static View from(Invoice invoice) {
            return new View(
                invoice.getId(),
                invoice.getTenantId(),
                invoice.getNumber(),
                invoice.getFiscalYear(),
                invoice.period(),
                invoice.getCurrency(),
                invoice.getTotalCents(),
                invoice.getIssuedAt(),
                invoice.getIssuedBy(),
                invoice.getSentAt(),
                invoice.getSentTo(),
                invoice.getLines().stream().map(LineView::from).toList()
            );
        }
    }
}
