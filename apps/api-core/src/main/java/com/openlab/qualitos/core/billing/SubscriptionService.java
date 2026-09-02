package com.openlab.qualitos.core.billing;

import com.openlab.qualitos.core.tenant.TenantNotFoundException;
import com.openlab.qualitos.core.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Souscrire, lire et résilier des abonnements — la vérité commerciale de la
 * plateforme.
 *
 * <p>Quatre règles qu'aucune contrainte SQL ne peut poser seule :
 *
 * <ol>
 *   <li><b>on ne souscrit pas un module sans tarif.</b> {@link ModulePriceService#priceOf}
 *       rend {@code Optional.empty()} pour un module hors catalogue ; en faire
 *       un abonnement à zéro euro ferait passer un tarif oublié pour un cadeau
 *       commercial, et la perte ne se verrait qu'à la lecture de la facture ;</li>
 *   <li><b>le prix est figé à la souscription</b>, recopié dans la ligne : une
 *       hausse du catalogue ne réécrit pas un contrat signé ;</li>
 *   <li><b>un module déjà souscrit ne se souscrit pas deux fois</b> — deux
 *       abonnements vivants pour le même module donneraient deux lignes de
 *       facture pour une seule prestation ;</li>
 *   <li><b>si le moteur n'ouvre pas le module, l'abonnement n'est pas
 *       enregistré</b> — sinon on facturerait un module que le client ne peut
 *       pas utiliser.</li>
 * </ol>
 *
 * <p><b>Ordre des opérations, et ce qu'il coûte quand ça casse.</b> Le moteur
 * est appelé AVANT l'écriture, dans les deux sens. Les deux bases sont
 * distinctes : aucune transaction ne les couvre ensemble, il faut donc choisir
 * laquelle des deux moitiés peut rester seule. Ouvrir puis échouer à
 * enregistrer laisse un module ouvert non facturé — un manque à gagner, visible
 * au prochain rapprochement. Enregistrer puis échouer à ouvrir laisserait un
 * client facturé pour un module fermé — une réclamation, et la confiance
 * perdue. On préfère perdre de l'argent que d'en réclamer indûment.
 */
@Service
@Transactional(readOnly = true)
public class SubscriptionService {

    private final SubscriptionRepository subscriptions;
    private final ModulePriceService prices;
    private final ModuleActivationPort activation;
    private final TenantRepository tenants;
    private final Clock clock;

    public SubscriptionService(SubscriptionRepository subscriptions,
                               ModulePriceService prices,
                               ModuleActivationPort activation,
                               TenantRepository tenants,
                               Clock clock) {
        this.subscriptions = subscriptions;
        this.prices = prices;
        this.activation = activation;
        this.tenants = tenants;
        this.clock = clock;
    }

    /**
     * Souscrit un module pour un client, à un palier et une périodicité.
     *
     * @param tenantId le client, désigné par le CHEMIN de la requête, jamais
     *                 par son corps (§18.2 règle 2)
     * @param actor    l'éditeur qui souscrit, lu du jeton (§18.2 règle 5)
     */
    @Transactional
    public SubscriptionDto.View subscribe(UUID tenantId,
                                          SubscriptionDto.SubscribeCommand command,
                                          UUID actor) {
        // On ne souscrit rien pour un client qui n'existe pas : la clé
        // étrangère le refuserait aussi, mais par une exception SQL générique
        // dont l'appelant ne saurait rien faire.
        if (!tenants.existsById(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
        // Règle 3, avant tout appel au moteur : inutile d'ouvrir un module que
        // le client a déjà souscrit.
        subscriptions.findLiveByTenantAndModule(tenantId, command.moduleCode())
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "Module deja souscrit par ce client : " + command.moduleCode());
                });
        // Règle 1 : pas de tarif, pas d'abonnement. Jamais Money.of(0, ...).
        Money price = prices
                .priceOf(command.moduleCode(), command.billingTier(), command.period())
                .orElseThrow(() -> new IllegalStateException(
                        "Aucun tarif au catalogue pour le module " + command.moduleCode()
                                + " (palier " + command.billingTier()
                                + ", periodicite " + command.period() + ")"));

        // Règle 4 : le moteur d'abord. S'il refuse, rien n'est écrit ici.
        activation.activate(tenantId, command.moduleCode());

        Instant now = Instant.now(clock);
        LocalDate startedOn = LocalDate.ofInstant(now, clock.getZone());
        Subscription subscription = Subscription.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .moduleCode(command.moduleCode())
                .billingTier(command.billingTier())
                .period(command.period())
                // Règle 2 : le prix est recopié, pas référencé.
                .amountCents(price.cents())
                .currency(price.currency())
                .startedOn(startedOn)
                .nextRenewal(command.period().nextRenewal(startedOn))
                .createdAt(now)
                .createdBy(actor)
                .build();

        return SubscriptionDto.View.from(subscriptions.save(subscription));
    }

    /**
     * Résilie un abonnement et ferme le module correspondant.
     *
     * <p>La ligne n'est PAS supprimée : elle est horodatée et attribuée.
     * L'historique justifie les factures déjà émises — effacer l'abonnement
     * rendrait inexplicable une ligne de facture de l'exercice précédent.
     *
     * <p>Fermer le module ferme l'ÉCRITURE, jamais la lecture : un client qui
     * résilie garde accès à ce qu'il a produit.
     */
    @Transactional
    public SubscriptionDto.View cancel(UUID tenantId, UUID subscriptionId, UUID actor) {
        Subscription subscription = subscriptions.findById(subscriptionId)
                .filter(candidate -> candidate.getTenantId().equals(tenantId))
                // Un abonnement qui appartient à un AUTRE client est traité en
                // « introuvable », et non en refus : sur le chemin du client A,
                // l'abonnement du client B n'existe pas. Répondre 403 confirmerait
                // au passage qu'il existe ailleurs — même raisonnement que le
                // `loadForTenant` du moteur de qualité. Ici la surface est déjà
                // réservée à l'éditeur, mais la règle protège surtout d'une
                // méprise : résilier le bon abonnement du mauvais client est une
                // erreur que rien d'autre ne rattraperait.
                .orElseThrow(() -> new SubscriptionNotFoundException(subscriptionId));
        if (!subscription.isLive()) {
            throw new IllegalStateException(
                    "Abonnement deja resilie : " + subscriptionId);
        }
        // Le moteur AVANT la mutation, et pas seulement avant le save() :
        // l'entité est gérée par la transaction, et la modifier suffirait à la
        // faire écrire au flush, même sans appel explicite au dépôt. Fermer
        // d'abord, muter ensuite, c'est le seul ordre où un refus du moteur
        // laisse vraiment l'abonnement intact.
        activation.deactivate(subscription.getTenantId(), subscription.getModuleCode());
        subscription.cancel(actor, Instant.now(clock));
        return SubscriptionDto.View.from(subscriptions.save(subscription));
    }

    /**
     * Les abonnements VIVANTS d'un client — ce sur quoi une facture se fonde.
     * Les résiliés en sont exclus : facturer un contrat clos est exactement le
     * genre d'erreur qu'un client relève avant nous.
     */
    public List<SubscriptionDto.View> activeFor(UUID tenantId) {
        return subscriptions.findLiveByTenant(tenantId).stream()
                .map(SubscriptionDto.View::from)
                .toList();
    }

}
