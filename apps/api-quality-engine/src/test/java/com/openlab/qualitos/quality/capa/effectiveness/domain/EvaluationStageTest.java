package com.openlab.qualitos.quality.capa.effectiveness.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'étape d'évaluation, CALCULÉE et jamais stockée (ADR 0108).
 *
 * <p>Une colonne {@code evaluation_stage} serait plus rapide à lire et fausse le
 * premier jour où une action est terminée par un autre chemin que l'écran
 * d'évaluation : il y aurait alors deux réponses à la même question, dont une
 * périmée. Ici l'étape se recalcule à chaque lecture depuis des faits qui, eux,
 * sont bien stockés.
 *
 * <p>Ce banc protège surtout le piège que j'ai introduit au brouillon : déduire
 * l'étape du SEUL dernier verdict figeait un dossier « action complémentaire
 * attendue » alors que l'action complémentaire était déjà terminée.
 */
class EvaluationStageTest {

    @Test
    void aCaseWhoseActionsAreAllDoneIsWaitingToBeJudged() {
        assertThat(EvaluationStage.of(false, true, null, 2, 2))
                .isEqualTo(EvaluationStage.AWAITING_EVALUATION);
    }

    @Test
    void aResolvedCaseWithAPendingActionIsStillAtWork() {
        // Rien n'interdit d'ajouter une action à un dossier résolu. L'écran doit
        // alors dire « en cours », pas « à juger » : proposer de juger un
        // dossier que le serveur refusera d'évaluer est un faux espoir.
        assertThat(EvaluationStage.of(false, true, null, 3, 2))
                .isEqualTo(EvaluationStage.ACTIONS_IN_PROGRESS);
    }

    @Test
    void aResolvedCaseWithoutAnyActionIsNotWaitingToBeJudged() {
        // Un dossier sans action n'a rien produit dont on puisse juger l'effet.
        // Un `done == total` naïf répondrait « oui » sur 0 == 0.
        assertThat(EvaluationStage.of(false, true, null, 0, 0))
                .isEqualTo(EvaluationStage.ACTIONS_IN_PROGRESS);
    }

    @Test
    void aClosedCaseIsClosedWhateverItsCounts() {
        assertThat(EvaluationStage.of(true, false, EffectivenessVerdict.EFFECTIVE, 2, 2))
                .isEqualTo(EvaluationStage.CLOSED_EFFECTIVE);
        assertThat(EvaluationStage.of(true, false, null, 0, 0))
                .isEqualTo(EvaluationStage.CLOSED_EFFECTIVE);
    }

    @Test
    void aPartialVerdictSendsTheCaseBackToWorkAndSaysWhy() {
        assertThat(EvaluationStage.of(false, false, EffectivenessVerdict.PARTIAL, 1, 1))
                .isEqualTo(EvaluationStage.COMPLEMENTARY_ACTION_EXPECTED);
    }

    @Test
    void anIneffectiveVerdictReopensTheCase() {
        assertThat(EvaluationStage.of(false, false, EffectivenessVerdict.INEFFECTIVE, 1, 1))
                .isEqualTo(EvaluationStage.REOPENED);
    }

    @Test
    void anAlreadyJudgedCaseThatIsResolvedAgainIsWaitingAgain() {
        // LE défaut du brouillon. Un dossier jugé PARTIEL repart au travail ;
        // quand son action complémentaire est faite et le dossier de nouveau
        // résolu, il attend un NOUVEAU jugement. Déduire l'étape du dernier
        // verdict seul l'aurait laissé « action complémentaire attendue » pour
        // toujours, et l'écran aurait refusé la seule chose à faire.
        assertThat(EvaluationStage.of(false, true, EffectivenessVerdict.PARTIAL, 2, 2))
                .isEqualTo(EvaluationStage.AWAITING_EVALUATION);
        assertThat(EvaluationStage.of(false, true, EffectivenessVerdict.INEFFECTIVE, 2, 2))
                .isEqualTo(EvaluationStage.AWAITING_EVALUATION);
    }

    @Test
    void aNeverJudgedCaseThatIsNotResolvedIsSimplyAtWork() {
        assertThat(EvaluationStage.of(false, false, null, 2, 1))
                .isEqualTo(EvaluationStage.ACTIONS_IN_PROGRESS);
    }

    @Test
    void anEffectiveVerdictOnAnOpenCaseFallsBackToWork() {
        // Un verdict EFFICACE clôt le dossier : le rencontrer sur un dossier
        // ouvert veut dire qu'il a été réouvert par un autre chemin. On annonce
        // le travail en cours plutôt que d'inventer une étape pour un cas que
        // le cycle normal ne produit pas.
        assertThat(EvaluationStage.of(false, false, EffectivenessVerdict.EFFECTIVE, 1, 1))
                .isEqualTo(EvaluationStage.ACTIONS_IN_PROGRESS);
    }

    @Test
    void onlyAwaitingEvaluationOpensThePanel() {
        assertThat(EvaluationStage.AWAITING_EVALUATION.allowsEvaluation()).isTrue();
        assertThat(EvaluationStage.ACTIONS_IN_PROGRESS.allowsEvaluation()).isFalse();
        assertThat(EvaluationStage.CLOSED_EFFECTIVE.allowsEvaluation()).isFalse();
        assertThat(EvaluationStage.COMPLEMENTARY_ACTION_EXPECTED.allowsEvaluation()).isFalse();
        assertThat(EvaluationStage.REOPENED.allowsEvaluation()).isFalse();
    }

    @Test
    void everythingButAClosedCaseStillCounts() {
        assertThat(EvaluationStage.CLOSED_EFFECTIVE.stillOpen()).isFalse();
        for (EvaluationStage stage : EvaluationStage.values()) {
            if (stage != EvaluationStage.CLOSED_EFFECTIVE) {
                assertThat(stage.stillOpen()).as("%s", stage).isTrue();
            }
        }
    }

    @Test
    void anIncoherentActionCountIsRefusedRatherThanRendered() {
        // Trois actions faites sur deux est un défaut d'appelant. L'afficher
        // produirait une étape plausible au-dessus d'un décompte faux, et
        // personne ne chercherait jamais la cause.
        assertThatThrownBy(() -> EvaluationStage.of(false, true, null, 2, 3))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3 / 2");
        assertThatThrownBy(() -> EvaluationStage.of(false, true, null, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> EvaluationStage.of(false, true, null, 2, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aCaseCannotBeBothClosedAndResolved() {
        assertThatThrownBy(() -> EvaluationStage.of(true, true, null, 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clos");
    }
}
