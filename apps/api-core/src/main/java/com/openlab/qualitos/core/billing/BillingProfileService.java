package com.openlab.qualitos.core.billing;

import com.openlab.qualitos.core.tenant.TenantNotFoundException;
import com.openlab.qualitos.core.tenant.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Applique au profil de facturation les règles qu'aucune contrainte SQL ne
 * peut, à elle seule, garantir avant l'écriture :
 *
 * <ol>
 *   <li>on ne crée un profil que pour un client qui existe ;</li>
 *   <li>un client n'a qu'un profil — un second enregistrement met à jour le
 *       premier, il n'en crée pas un autre ;</li>
 *   <li>une exemption sans motif est refusée ;</li>
 *   <li>l'absence de profil n'est PAS une exemption.</li>
 * </ol>
 *
 * <p>{@link Clock} injecté plutôt que {@code Instant.now()} : les tests
 * fixent l'horloge pour affirmer des horodatages exacts, et un déploiement
 * multi-fuseau reste correct puisque {@code Clock.systemUTC()} est la seule
 * horloge posée en production.
 */
@Service
@Transactional(readOnly = true)
public class BillingProfileService {

    private final BillingProfileRepository billingProfiles;
    private final TenantRepository tenants;
    private final Clock clock;

    // Deux constructeurs plutôt qu'un bean Clock séparé : celui-ci est le
    // seul que Spring voit (@Autowired), il pose l'horloge système UTC — la
    // seule horloge posée en production, quel que soit le fuseau du serveur.
    @Autowired
    public BillingProfileService(BillingProfileRepository billingProfiles, TenantRepository tenants) {
        this(billingProfiles, tenants, Clock.systemUTC());
    }

    // Non public : réservé aux tests, qui figent l'horloge pour affirmer des
    // horodatages exacts. Spring ne le voit pas (un seul constructeur peut
    // porter @Autowired) et ne tentera donc jamais de résoudre un bean Clock.
    BillingProfileService(BillingProfileRepository billingProfiles, TenantRepository tenants, Clock clock) {
        this.billingProfiles = billingProfiles;
        this.tenants = tenants;
        this.clock = clock;
    }

    @Transactional
    public BillingProfileDto.View upsert(UUID tenantId, BillingProfileDto.SaveCommand command) {
        // Règle 1 : pas de profil orphelin — aucune facture ne pourrait le
        // rattacher à un client réel.
        if (!tenants.existsById(tenantId)) {
            throw new TenantNotFoundException(tenantId);
        }
        // Règle 3 : une exemption sans motif est une anomalie qu'un audit
        // relèvera. Vérifiée ici et non seulement en base : on veut le refus
        // avant l'écriture, pas une exception SQL générique en retour.
        if (command.billingExempt() && isBlank(command.exemptionReason())) {
            throw new IllegalArgumentException(
                    "Une exemption de facturation doit indiquer un motif");
        }

        Instant now = Instant.now(clock);
        // Règle 2 : un client n'a qu'un profil. On réutilise l'existant s'il
        // y en a un, on n'en crée jamais un second.
        BillingProfile profile = billingProfiles.findByTenantId(tenantId)
                .orElseGet(() -> BillingProfile.builder()
                        .id(UUID.randomUUID())
                        .tenantId(tenantId)
                        .createdAt(now)
                        .build());

        profile.setLegalName(command.legalName());
        profile.setVatNumber(command.vatNumber());
        profile.setAddressLine1(command.addressLine1());
        profile.setAddressLine2(command.addressLine2());
        profile.setPostalCode(command.postalCode());
        profile.setCity(command.city());
        profile.setCountryCode(command.countryCode());
        profile.setBillingEmail(command.billingEmail());
        profile.setCurrency(command.currency());
        profile.setBillingExempt(command.billingExempt());
        profile.setExemptionReason(command.exemptionReason());
        profile.setUpdatedAt(now);

        return BillingProfileDto.View.from(billingProfiles.save(profile));
    }

    public Optional<BillingProfileDto.View> find(UUID tenantId) {
        return billingProfiles.findByTenantId(tenantId).map(BillingProfileDto.View::from);
    }

    /**
     * Règle 4, la plus importante : l'ABSENCE de profil n'exempte de rien.
     * Un client sans profil est un profil à remplir, pas un client dispensé
     * de facturation — l'inverse tairait silencieusement la facturation d'un
     * client réel.
     */
    public boolean isExempt(UUID tenantId) {
        return billingProfiles.findByTenantId(tenantId)
                .map(BillingProfile::isBillingExempt)
                .orElse(false);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
