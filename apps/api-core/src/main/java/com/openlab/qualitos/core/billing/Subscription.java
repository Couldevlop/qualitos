package com.openlab.qualitos.core.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
import java.time.LocalDate;
import java.util.UUID;

/**
 * Ce qu'un client a souscrit : un module, à un palier, dans une périodicité,
 * à un prix. C'est la vérité COMMERCIALE ; l'activation du module dans le
 * moteur de qualité n'en est que la conséquence technique.
 *
 * <p><b>Le montant est figé ici, il n'est pas relu du tarif courant.</b>
 * {@code amountCents} et {@code currency} sont recopiés depuis
 * {@link ModulePriceService#priceOf} au moment de la souscription et ne
 * bougent plus. Sans cette copie, une hausse du catalogue réécrirait
 * rétroactivement le montant d'un contrat signé — et celui des factures déjà
 * émises, qui lisent l'abonnement. Un client verrait le prix de son contrat
 * changer sans avoir rien signé.
 *
 * <p><b>Une résiliation ne supprime pas la ligne</b> : elle horodate
 * {@code cancelledAt} et nomme {@code cancelledBy}. L'historique justifie les
 * factures passées — effacer l'abonnement rendrait inexplicable une ligne de
 * facture émise l'an dernier. L'index partiel {@code uk_subscription_vivante}
 * traduit exactement cela : un seul abonnement VIVANT par (client, module),
 * autant de résiliés que le temps en produira.
 */
@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

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

    // updatable = false : le prix convenu ne se corrige pas en place. Changer
    // le tarif d'un client, c'est résilier puis re-souscrire — deux actes
    // datés et attribués, pas une colonne réécrite en silence.
    @PositiveOrZero
    @Column(name = "amount_cents", nullable = false, updatable = false)
    private long amountCents;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    @Column(name = "currency", nullable = false, length = 3, updatable = false)
    private String currency;

    @NotNull
    @Column(name = "started_on", nullable = false, updatable = false)
    private LocalDate startedOn;

    // La seule colonne d'un abonnement vivant qui bouge : chaque échéance
    // passée la repousse d'une période.
    @NotNull
    @Column(name = "next_renewal", nullable = false)
    private LocalDate nextRenewal;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancelled_by")
    private UUID cancelledBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    /** Un abonnement vivant est un abonnement non résilié. Rien de plus. */
    public boolean isLive() {
        return cancelledAt == null;
    }

    /**
     * Résilie l'abonnement, à une date et par un acteur nommés.
     *
     * <p>Les deux ensemble ou aucun des deux (contrainte SQL
     * {@code chk_sub_cancellation_complete}) : une résiliation sans auteur
     * laisserait un contrat fermé que personne n'aurait fermé.
     *
     * <p>Une seconde résiliation est refusée plutôt qu'ignorée : réécrire la
     * date effacerait celle qui fait foi devant le client.
     */
    public void cancel(UUID actor, Instant when) {
        if (!isLive()) {
            throw new IllegalStateException(
                    "Abonnement deja resilie le " + cancelledAt + " : " + id);
        }
        this.cancelledAt = when;
        this.cancelledBy = actor;
    }
}
