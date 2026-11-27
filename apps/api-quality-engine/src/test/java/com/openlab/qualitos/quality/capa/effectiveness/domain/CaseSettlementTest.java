package com.openlab.qualitos.quality.capa.effectiveness.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Le verdict d'une action solde-t-il son dossier (ADR 0143) ? */
class CaseSettlementTest {

    /**
     * LE CAS DE LA RECETTE : l'action a été jugée efficace depuis son
     * discipline 8D, le dossier est résolu, rien ne s'oppose à la clôture. Une
     * seconde évaluation du même geste n'aurait rien appris de plus.
     */
    @Test
    void unVerdictEfficaceClotUnDossierResoluSansObstacle() {
        assertThat(CaseSettlement.settlesCase(EffectivenessVerdict.EFFECTIVE, true, () -> true)).isTrue();
    }

    /** « Partiellement » demande la suite du travail : il ne ferme rien. */
    @Test
    void unVerdictPartielNeClotRien() {
        assertThat(CaseSettlement.settlesCase(EffectivenessVerdict.PARTIAL, true, () -> true)).isFalse();
    }

    @Test
    void unVerdictInefficaceNeClotRien() {
        assertThat(CaseSettlement.settlesCase(EffectivenessVerdict.INEFFECTIVE, true, () -> true))
                .isFalse();
    }

    @Test
    void sansVerdictRienNeSeSolde() {
        assertThat(CaseSettlement.settlesCase(null, true, () -> true)).isFalse();
    }

    /**
     * Un dossier qui n'est pas résolu porte encore un travail en cours : c'est
     * déjà la condition du verdict porté sur le dossier (ADR 0108), et elle ne
     * s'assouplit pas parce que le jugement vient d'une action.
     */
    @Test
    void unDossierNonResoluNeSeClotPas() {
        assertThat(CaseSettlement.settlesCase(EffectivenessVerdict.EFFECTIVE, false, () -> true))
                .isFalse();
    }

    /**
     * UN OBSTACLE RESTANT N'EST PAS CONTOURNE. Sans cela, juger une action
     * serait la porte dérobée par laquelle l'écart encore ouvert, l'autre
     * vérification promise ou l'objectif qualité non revenu à la cible se
     * franchiraient (ADR 0137).
     */
    @Test
    void unObstacleALaClotureRetientLeDossier() {
        assertThat(CaseSettlement.settlesCase(EffectivenessVerdict.EFFECTIVE, true, () -> false))
                .isFalse();
    }

    /**
     * LA LISTE D'OBSTACLES NE S'ETABLIT QUE SI ELLE DECIDE DE QUELQUE CHOSE :
     * elle coûte des lectures — l'écart rattaché, les autres vérifications
     * promises, l'objectif qualité — et un verdict qui ne clôt rien n'a aucune
     * raison de les payer.
     */
    @Test
    void lesObstaclesNeSeLisentPasQuandLeVerdictNeClotRien() {
        boolean[] demande = { false };
        CaseSettlement.settlesCase(EffectivenessVerdict.PARTIAL, true, () -> {
            demande[0] = true;
            return true;
        });
        assertThat(demande[0]).isFalse();
    }
}
