package com.openlab.qualitos.quality.tenantmodules.infrastructure;

import com.openlab.qualitos.quality.tenantmodules.application.TenantTierProvider;
import com.openlab.qualitos.quality.tenantmodules.domain.BillingTier;

import java.util.Locale;
import java.util.UUID;

/**
 * Palier de facturation appliqué à tous les tenants, en attendant un module de
 * facturation qui le portera par tenant.
 *
 * <p>Il était figé à {@code FREE} dans le code. Conséquence concrète : même le
 * super administrateur ne pouvait ouvrir aucun module de palier supérieur — le
 * service refusait l'activation faute de palier suffisant — et « donner le
 * standard à ce tenant » n'avait aucun moyen d'être satisfait sans recompiler.
 *
 * <p>Un palier inconnu empêche le démarrage : retomber en silence sur {@code FREE}
 * laisserait croire le réglage pris en compte, et le refus d'activation qui
 * s'ensuivrait serait incompréhensible.
 */
public final class ConfiguredTenantTierProvider implements TenantTierProvider {

    private final BillingTier tier;

    public ConfiguredTenantTierProvider(String configured) {
        String raw = configured == null ? "" : configured.trim();
        if (raw.isEmpty()) {
            this.tier = BillingTier.FREE;
            return;
        }
        try {
            this.tier = BillingTier.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "qualitos.modules.default-tier invalide : '" + raw
                            + "' (attendu : FREE | STANDARD | PRO | ENTERPRISE)", e);
        }
    }

    @Override
    public BillingTier currentTier(UUID tenantId) {
        return tier;
    }
}
