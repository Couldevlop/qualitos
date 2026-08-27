package com.openlab.qualitos.quality.pdca;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Revendication des binaires PDCA face au balayeur d'orphelins (§4.3).
 *
 * <p>Sans cet adaptateur, le balayage prendrait toute preuve d'étape pour un
 * orphelin passé le délai de grâce et l'effacerait. Ce qui se teste ici est donc
 * moins une conversion qu'un garde-fou : la réponse « oui, je le revendique »
 * doit suivre exactement l'existence d'une ligne.
 */
@ExtendWith(MockitoExtension.class)
class PdcaStepEvidenceObjectOwnerTest {

    @Mock PdcaStepEvidenceRepository evidences;

    @Test
    void revendique_unObjetEncoreDesigneParUneLigne() {
        PdcaStepEvidenceObjectOwner owner = new PdcaStepEvidenceObjectOwner(evidences);
        when(evidences.existsByObjectKey("tenants/t/pdca/c/steps/s/a.pdf")).thenReturn(true);

        assertThat(owner.isReferenced("tenants/t/pdca/c/steps/s/a.pdf")).isTrue();
    }

    @Test
    void abandonne_unObjetQuePlusAucuneLigneNeDesigne() {
        PdcaStepEvidenceObjectOwner owner = new PdcaStepEvidenceObjectOwner(evidences);
        when(evidences.existsByObjectKey("tenants/t/pdca/c/steps/s/perdu.pdf")).thenReturn(false);

        assertThat(owner.isReferenced("tenants/t/pdca/c/steps/s/perdu.pdf")).isFalse();
    }

    /**
     * Le nom sert au journal du balayage : il doit désigner le module, faute de
     * quoi une suppression massive resterait sans coupable identifiable.
     */
    @Test
    void seNomme_pourLeJournalDuBalayage() {
        assertThat(new PdcaStepEvidenceObjectOwner(evidences).ownerName())
                .isEqualTo("pdca-step-evidence");
    }

    // --- rappel d'entité ------------------------------------------------------

    @Test
    void horodate_laPieceAuVersement() {
        PdcaStepEvidence e = new PdcaStepEvidence();

        e.prePersist();

        assertThat(e.getCreatedAt()).isNotNull();
    }

    @Test
    void conserve_unHorodatageDejaPose() {
        // La reprise d'un historique doit pouvoir imposer sa date : l'écraser
        // ferait passer une pièce ancienne pour une pièce versée aujourd'hui.
        PdcaStepEvidence e = new PdcaStepEvidence();
        Instant hier = Instant.parse("2026-08-26T08:00:00Z");
        e.setCreatedAt(hier);

        e.prePersist();

        assertThat(e.getCreatedAt()).isEqualTo(hier);
    }
}
