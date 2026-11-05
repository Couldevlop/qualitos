package com.openlab.qualitos.core.tenant;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Les couleurs d'un client ne sont pas une affaire de goût : une couleur
 * d'accent choisie trop pâle rend illisibles les boutons de toute la
 * plateforme, pour tous ses utilisateurs, sans que personne ne puisse y
 * remédier depuis l'écran. Le contraste se mesure donc au serveur, selon la
 * formule WCAG 2.1, avant d'être enregistré.
 */
class ContrastGuardTest {

    @Test
    void contrasteDuNoirSurBlancEstMaximal() {
        assertThat(ContrastGuard.ratio("#000000", "#FFFFFF")).isCloseTo(21.0, within(0.05));
    }

    @Test
    void contrasteDUneCouleurAvecElleMemeEstNeutre() {
        assertThat(ContrastGuard.ratio("#0B6B5C", "#0B6B5C")).isCloseTo(1.0, within(0.001));
    }

    @Test
    void lOrdreDesCouleursNeChangeRien() {
        assertThat(ContrastGuard.ratio("#0B6B5C", "#FFFFFF"))
                .isCloseTo(ContrastGuard.ratio("#FFFFFF", "#0B6B5C"), within(0.001));
    }

    @Test
    void couleurTropPaleRefusee() {
        assertThatThrownBy(() -> ContrastGuard.requireReadable("#EEEEEE", "#FFFFFF"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contraste");
    }

    @Test
    void couleurDAccentDuSocleAcceptee() {
        // Le vert de QualitOS sur fond blanc : c'est la référence à ne pas casser.
        ContrastGuard.requireReadable("#0B6B5C", "#FFFFFF");
    }

    @Test
    void hexInvalideRefuseAvantTouteMesure() {
        assertThatThrownBy(() -> ContrastGuard.ratio("rouge", "#FFFFFF"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hexadécimale");
    }

    @Test
    void formeCourteRefusee() {
        // #FFF est valide en CSS, mais accepter deux écritures pour la même
        // couleur ferait diverger la comparaison et le stockage.
        assertThatThrownBy(() -> ContrastGuard.ratio("#FFF", "#000000"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void absenceDeCouleurRefusee() {
        assertThatThrownBy(() -> ContrastGuard.ratio(null, "#000000"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void minusculesEtMajusculesDonnentLeMemeResultat() {
        assertThat(ContrastGuard.ratio("#0b6b5c", "#ffffff"))
                .isCloseTo(ContrastGuard.ratio("#0B6B5C", "#FFFFFF"), within(0.001));
    }

    @Test
    void leSeuilExposeEstCeluiDeLaNormeAA() {
        assertThat(ContrastGuard.MINIMUM_RATIO).isEqualTo(4.5);
    }
}
