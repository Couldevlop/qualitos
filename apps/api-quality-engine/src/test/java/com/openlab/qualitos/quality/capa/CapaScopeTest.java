package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.authz.domain.Permission;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Ce qui fait qu'un dossier CAPA concerne quelqu'un (ADR 0081). */
class CapaScopeTest {

    final UUID moi = UUID.randomUUID();

    CapaCase dossier() {
        CapaCase c = new CapaCase();
        c.setOwnerId(UUID.randomUUID());
        return c;
    }

    @Test
    void voirToutLaisseToutPasser() {
        assertThat(CapaScope.sees(Optional.empty(), dossier())).isTrue();
    }

    @Test
    void piloterVerifierOuPorterUneActionConcerne() {
        CapaCase pilote = dossier();
        pilote.setOwnerId(moi);
        CapaCase verifie = dossier();
        verifie.setVerificationAssigneeId(moi);
        CapaCase action = dossier();
        CapaAction a = new CapaAction();
        a.setAssigneeId(moi);
        action.getActions().add(a);
        CapaAction autre = new CapaAction();
        autre.setAssigneeId(UUID.randomUUID());
        CapaCase etranger = dossier();
        etranger.getActions().add(autre);

        Optional<UUID> seulement = Optional.of(moi);
        assertThat(CapaScope.sees(seulement, pilote)).isTrue();
        assertThat(CapaScope.sees(seulement, verifie)).isTrue();
        assertThat(CapaScope.sees(seulement, action)).isTrue();
        assertThat(CapaScope.sees(seulement, etranger)).isFalse();
        assertThat(CapaScope.sees(seulement, dossier())).isFalse();
    }

    @Test
    void laPorteeSeDemandeAvecVoirTousLesDossiers() {
        Permission[] demande = new Permission[1];
        CapaScope.restriction(p -> {
            demande[0] = p;
            return Optional.empty();
        });
        assertThat(demande[0]).isEqualTo(Permission.CAPA_VIEW_ALL);
    }
}
