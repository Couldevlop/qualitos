package com.openlab.qualitos.quality.capa.effectiveness.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le verdict d'efficacité à trois états (ADR 0108).
 *
 * <p>Ce que ce banc protège, c'est que les CONSÉQUENCES voyagent avec le verdict
 * plutôt que d'être redécidées à chaque appelant. Un {@code switch} sur l'énum
 * écrit dans le service, un autre dans le contrôleur, un troisième dans le
 * tableau, et le jour où PARTIAL se met à clore un dossier quelque part, rien ne
 * le dit. Ici, {@link EffectivenessVerdict#closesCase()} est la seule réponse.
 */
class EffectivenessVerdictTest {

    @Test
    void onlyAnEffectiveVerdictClosesTheCase() {
        assertThat(EffectivenessVerdict.EFFECTIVE.closesCase()).isTrue();
        assertThat(EffectivenessVerdict.PARTIAL.closesCase()).isFalse();
        assertThat(EffectivenessVerdict.INEFFECTIVE.closesCase()).isFalse();
    }

    @Test
    void onlyAPartialVerdictExpectsAComplementaryAction() {
        // C'est tout l'apport du troisième état : INEFFICACE dit « ça n'a pas
        // marché », PARTIEL dit « ça a marché à moitié, il manque un geste ».
        // Un booléen ne pouvait pas distinguer les deux, et obligeait à mentir
        // dans un sens ou dans l'autre.
        assertThat(EffectivenessVerdict.PARTIAL.complementaryActionExpected()).isTrue();
        assertThat(EffectivenessVerdict.EFFECTIVE.complementaryActionExpected()).isFalse();
        assertThat(EffectivenessVerdict.INEFFECTIVE.complementaryActionExpected()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(EffectivenessVerdict.class)
    void noVerdictBothClosesAndExpectsMore(EffectivenessVerdict verdict) {
        // Clore et attendre une action de plus sont contradictoires : la
        // combinaison n'a aucun sens métier, et un quatrième état ajouté par
        // distraction la rendrait possible sans que personne ne le remarque.
        assertThat(verdict.closesCase() && verdict.complementaryActionExpected()).isFalse();
    }

    @Test
    void theLegacyFlagIsAProjectionOfTheVerdict() {
        // L'ancien booléen reste écrit en base pour les lecteurs existants, mais
        // il n'est plus une source : il se DÉDUIT. PARTIEL y vaut false, parce
        // que ce champ n'a jamais su dire autre chose que oui ou non.
        assertThat(EffectivenessVerdict.EFFECTIVE.asLegacyVerifiedFlag()).isTrue();
        assertThat(EffectivenessVerdict.PARTIAL.asLegacyVerifiedFlag()).isFalse();
        assertThat(EffectivenessVerdict.INEFFECTIVE.asLegacyVerifiedFlag()).isFalse();
    }

    @Test
    void theLegacyFlagNeverProducesAPartialVerdict() {
        // Traduire false en PARTIEL inventerait une nuance que l'appelant n'a
        // pas exprimée, et inscrirait au registre un jugement que personne n'a
        // rendu. L'ancienne API ne connaît que deux réponses : on en garde deux.
        assertThat(EffectivenessVerdict.fromLegacyFlag(true)).isEqualTo(EffectivenessVerdict.EFFECTIVE);
        assertThat(EffectivenessVerdict.fromLegacyFlag(false))
                .isEqualTo(EffectivenessVerdict.INEFFECTIVE);
    }

    @Test
    void theLegacyRoundTripIsLosslessForTheTwoStatesItKnows() {
        for (boolean flag : new boolean[]{true, false}) {
            assertThat(EffectivenessVerdict.fromLegacyFlag(flag).asLegacyVerifiedFlag())
                    .isEqualTo(flag);
        }
    }
}
