package com.openlab.qualitos.core.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * Le tarif d'un module, pour un palier commercial et une périodicité donnés.
 *
 * <p>En BASE et non en constante du code (voir {@code V5__create_module_prices.sql}) :
 * un tarif change sans livraison, et le changer par un déploiement ferait
 * dépendre la politique commerciale du cycle de release.
 *
 * <p>{@code amountCents} porte le prix ANNUEL comme le prix MENSUEL — chaque
 * ligne est un couple (palier, périodicité) distinct. Le prix annuel n'est
 * JAMAIS déduit du mensuel par une multiplication par douze : une remise
 * annuelle est une décision commerciale, que seule une ligne dédiée peut
 * fixer (voir {@link ModulePriceService#priceOf}).
 *
 * <p>{@code moduleCode}, {@code billingTier} et {@code period} forment la clé
 * naturelle (contrainte {@code uk_module_price}) et ne changent jamais après
 * création — seuls le montant, la devise et les traces d'audit bougent — d'où
 * {@code updatable = false} sur ces trois colonnes.
 */
@Entity
@Table(
    name = "module_prices",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_module_price", columnNames = {"module_code", "billing_tier", "period"})
    }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModulePrice {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

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

    // Un prix nul est légitime (palier FREE) ; un prix négatif ne l'est pas —
    // même règle que la contrainte SQL chk_price_amount.
    @PositiveOrZero
    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    // ISO 4217, même forme que Money (billing.Money) et BillingProfile.currency.
    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Qui a fixé ce tarif — obligatoire : un tarif décidé par personne
    // n'existe pas, c'est une politique commerciale qui engage un éditeur
    // identifié (voir ModulePriceController).
    @NotNull
    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;
}
