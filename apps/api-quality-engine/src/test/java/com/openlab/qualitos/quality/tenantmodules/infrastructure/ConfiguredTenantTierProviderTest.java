package com.openlab.qualitos.quality.tenantmodules.infrastructure;

import com.openlab.qualitos.quality.tenantmodules.application.TenantTierProvider;
import com.openlab.qualitos.quality.tenantmodules.domain.BillingTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Palier de facturation par défaut, en attendant un module de facturation.
 *
 * <p>Il était figé à {@code FREE} dans le code. Conséquence : même le super
 * administrateur ne pouvait ouvrir aucun module de palier supérieur — le service
 * refusait l'activation faute de palier suffisant —, et la demande « donner le
 * standard à ce tenant » n'avait aucun moyen d'être satisfaite sans recompiler.
 * Le palier devient un réglage (§18.2-9 : rien de sectoriel ni de contractuel
 * codé en dur), FREE restant le défaut prudent.
 */
class ConfiguredTenantTierProviderTest {

    private static final UUID TENANT = UUID.randomUUID();

    @Test
    @DisplayName("sans réglage, le palier reste FREE")
    void defaultsToFree() {
        TenantTierProvider provider = new ConfiguredTenantTierProvider("");

        assertThat(provider.currentTier(TENANT)).isEqualTo(BillingTier.FREE);
    }

    @Test
    @DisplayName("le palier configuré s'applique à tous les tenants")
    void appliesTheConfiguredTier() {
        TenantTierProvider provider = new ConfiguredTenantTierProvider("STANDARD");

        assertThat(provider.currentTier(TENANT)).isEqualTo(BillingTier.STANDARD);
    }

    @Test
    @DisplayName("la casse et les espaces sont tolérés")
    void toleratesCasingAndSpaces() {
        assertThat(new ConfiguredTenantTierProvider("  pro ").currentTier(TENANT))
                .isEqualTo(BillingTier.PRO);
    }

    @Test
    @DisplayName("un palier inconnu empêche le démarrage plutôt que de se dégrader")
    void unknownTierFailsFast() {
        // Retomber en silence sur FREE ferait croire à un réglage pris en compte,
        // et le refus d'activation qui s'ensuivrait serait incompréhensible.
        assertThatThrownBy(() -> new ConfiguredTenantTierProvider("PREMIUM"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PREMIUM");
    }
}
