package com.openlab.qualitos.quality.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le passage dans un client désigné, et surtout le retour.
 *
 * <p>Ce qui compte ici n'est pas d'entrer — c'est de ressortir dans l'état où
 * l'on était, y compris quand le traitement échoue. Un contexte laissé à
 * l'abandon ferait travailler la suite de la requête chez le mauvais client,
 * ou sans client du tout : c'est précisément ce que l'isolation par la base
 * est censée empêcher.
 */
class TenantScopeTest {

    private static final UUID APPELANT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID DESIGNE = UUID.fromString("00000000-0000-0000-0000-0000000000b2");

    @AfterEach
    void nettoie() {
        TenantContext.clear();
    }

    @Test
    void leTraitementVoitLeClientDesigne() {
        TenantContext.setTenantId(APPELANT.toString());

        String vu = TenantScope.callAs(DESIGNE, TenantContext::getTenantId);

        assertThat(vu).isEqualTo(DESIGNE.toString());
    }

    @Test
    void leClientDeLAppelantEstRenduApres() {
        TenantContext.setTenantId(APPELANT.toString());

        TenantScope.runAs(DESIGNE, () -> { });

        assertThat(TenantContext.getTenantId()).isEqualTo(APPELANT.toString());
    }

    @Test
    void leClientDeLAppelantEstRenduMemeQuandLeTraitementEchoue() {
        TenantContext.setTenantId(APPELANT.toString());

        assertThatThrownBy(() -> TenantScope.runAs(DESIGNE, () -> {
            throw new IllegalStateException("écriture refusée");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(TenantContext.getTenantId()).isEqualTo(APPELANT.toString());
    }

    @Test
    void sansClientAuDepartOnRessortSansClient() {
        // Le cas des travaux de fond : ils parcourent les clients un par un et
        // ne doivent pas rester accrochés au dernier visité.
        assertThat(TenantContext.hasTenant()).isFalse();

        TenantScope.runAs(DESIGNE, () -> { });

        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void unClientDesigneAbsentEstRefuse() {
        assertThatThrownBy(() -> TenantScope.runAs(null, () -> { }))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("client désigné");
    }

    @Test
    void leResultatDuTraitementEstRendu() {
        assertThat(TenantScope.callAs(DESIGNE, () -> 42)).isEqualTo(42);
    }
}
