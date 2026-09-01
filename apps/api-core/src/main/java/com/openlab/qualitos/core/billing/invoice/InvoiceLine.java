package com.openlab.qualitos.core.billing.invoice;

import com.openlab.qualitos.core.billing.BillingPeriod;
import com.openlab.qualitos.core.billing.BillingTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Une ligne de facture : ce qui est facturé, en quelle quantité, à quel prix.
 *
 * <p>{@code lineTotalCents} est stocké, alors qu'il vaut toujours
 * {@code unitAmountCents × quantity}. C'est une redondance assumée : c'est le
 * montant IMPRIMÉ sur la pièce remise au client, et une pièce comptable doit
 * pouvoir être relue telle qu'elle a été émise, sans être recalculée. La
 * contrainte SQL {@code chk_line_product} interdit qu'il diverge du produit —
 * on garde la redondance, pas l'incohérence.
 *
 * <p>Aucune référence JPA vers {@code Subscription} : l'abonnement peut être
 * purgé au départ d'un client, la facture, non. Seul son identifiant est
 * recopié, pour la piste d'audit.
 */
@Entity
@Table(name = "invoice_lines")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceLine {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "subscription_id", nullable = false, updatable = false)
    private UUID subscriptionId;

    @Column(name = "line_no", nullable = false, updatable = false)
    private int lineNo;

    @NotBlank
    @Size(max = 64)
    @Column(name = "module_code", nullable = false, length = 64, updatable = false)
    private String moduleCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_tier", nullable = false, length = 32, updatable = false)
    private BillingTier billingTier;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false, length = 16, updatable = false)
    private BillingPeriod period;

    @Positive
    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @PositiveOrZero
    @Column(name = "unit_amount_cents", nullable = false, updatable = false)
    private long unitAmountCents;

    @PositiveOrZero
    @Column(name = "line_total_cents", nullable = false, updatable = false)
    private long lineTotalCents;
}
