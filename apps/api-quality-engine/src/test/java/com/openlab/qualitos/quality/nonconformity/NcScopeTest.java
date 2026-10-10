package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.authz.domain.Permission;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Une NC concerne celui qui l'a déclarée (ADR 0081). */
class NcScopeTest {

    @Test
    void seulLeDeclarantLaVoitSansVoirTout() {
        UUID moi = UUID.randomUUID();
        NonConformity sienne = new NonConformity();
        sienne.setReporterId(moi);
        NonConformity autre = new NonConformity();
        autre.setReporterId(UUID.randomUUID());
        NonConformity sansDeclarant = new NonConformity();

        assertThat(NcScope.sees(Optional.of(moi), sienne)).isTrue();
        assertThat(NcScope.sees(Optional.of(moi), autre)).isFalse();
        assertThat(NcScope.sees(Optional.of(moi), sansDeclarant)).isFalse();
        assertThat(NcScope.sees(Optional.empty(), autre)).isTrue();
    }

    @Test
    void laPorteeSeDemandeAvecVoirToutesLesNc() {
        Permission[] demande = new Permission[1];
        NcScope.restriction(p -> {
            demande[0] = p;
            return Optional.empty();
        });
        assertThat(demande[0]).isEqualTo(Permission.NC_VIEW_ALL);
    }
}
