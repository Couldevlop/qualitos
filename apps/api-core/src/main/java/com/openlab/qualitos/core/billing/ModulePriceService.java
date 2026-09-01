package com.openlab.qualitos.core.billing;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Le tarif des modules : ce qui répond à "combien coûte tel module, pour tel
 * palier, dans telle périodicité ?".
 *
 * <ol>
 *   <li>le prix ANNUEL est stocké, jamais calculé depuis le mensuel — une
 *       remise annuelle est une décision commerciale, pas une multiplication
 *       par douze (voir {@link #priceOf}) ;</li>
 *   <li>un module sans tarif ne se facture pas, et le dit — {@link #priceOf}
 *       rend {@link Optional#empty()}, jamais {@code Money.of(0, ...)}, qui
 *       ferait passer un tarif oublié pour un module gratuit.</li>
 * </ol>
 *
 * <p>{@link Clock} injecté plutôt que {@code Instant.now()} : même motif que
 * {@link BillingProfileService} — les tests fixent l'horloge pour affirmer des
 * horodatages exacts, et {@code Clock.systemUTC()} (bean unique de
 * {@link com.openlab.qualitos.core.config.ClockConfig}) est la seule horloge
 * posée en production.
 */
@Service
@Transactional(readOnly = true)
public class ModulePriceService {

    private final ModulePriceRepository modulePrices;
    private final Clock clock;

    // Constructeur unique, comme BillingProfileService : Spring l'utilise
    // directement, les tests lui fournissent leur propre horloge fixe par le
    // même constructeur.
    public ModulePriceService(ModulePriceRepository modulePrices, Clock clock) {
        this.modulePrices = modulePrices;
        this.clock = clock;
    }

    /**
     * Le tarif d'un module, pour un palier et une périodicité donnés.
     *
     * <p>Règle 2 (la plus dangereuse à enfreindre) : {@code Optional.empty()}
     * quand aucun tarif n'existe, JAMAIS {@code Money.of(0, ...)}. Rendre zéro
     * silencieusement ferait passer un tarif oublié pour un module gratuit, et
     * la perte ne se verrait qu'à la lecture de la facture.
     */
    public Optional<Money> priceOf(String moduleCode, BillingTier tier, BillingPeriod period) {
        return modulePrices.find(moduleCode, tier, period)
                .map(price -> Money.of(price.getAmountCents(), price.getCurrency()));
    }

    /**
     * Le catalogue complet des tarifs, trié par module puis palier puis
     * périodicité — l'ordre dans lequel un catalogue commercial se lit.
     */
    public List<ModulePriceDto.View> findAll() {
        return modulePrices.findAll(Sort.by("moduleCode", "billingTier", "period"))
                .stream()
                .map(ModulePriceDto.View::from)
                .toList();
    }

    /**
     * Fixe (crée ou remplace) le tarif d'un module pour un palier et une
     * périodicité. Un second appel sur le même triplet (module, palier,
     * période) — la clé naturelle protégée par {@code uk_module_price} — met
     * à jour la ligne existante, il n'en crée jamais une seconde : deux
     * tarifs pour le même triplet rendraient {@link #priceOf} ambigu.
     *
     * @param actor l'éditeur qui fixe ce tarif, résolu par
     *              {@link ModulePriceController} depuis le jeton
     *              d'authentification — jamais depuis le corps de la requête.
     */
    @Transactional
    public ModulePriceDto.View setPrice(ModulePriceDto.SaveCommand command, UUID actor) {
        Instant now = Instant.now(clock);
        ModulePrice price = modulePrices
                .find(command.moduleCode(), command.billingTier(), command.period())
                .orElseGet(() -> ModulePrice.builder()
                        .id(UUID.randomUUID())
                        .moduleCode(command.moduleCode())
                        .billingTier(command.billingTier())
                        .period(command.period())
                        .build());

        price.setAmountCents(command.amountCents());
        price.setCurrency(command.currency());
        price.setUpdatedAt(now);
        price.setUpdatedBy(actor);

        return ModulePriceDto.View.from(modulePrices.save(price));
    }
}
