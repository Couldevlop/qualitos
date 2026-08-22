package com.openlab.qualitos.quality.capa.effectiveness.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'efficacité d'une CAPA, mesurée et non déclarée.
 *
 * <p>Le module CAPA sait déjà enregistrer qu'un responsable a coché « efficacité
 * vérifiée ». C'est une opinion. Ce calcul-ci compare ce qui est arrivé AVANT
 * l'ouverture du dossier à ce qui est arrivé APRÈS sa clôture, sur une fenêtre de
 * même durée : le terrain répond à la place du responsable.
 *
 * <p>Trois refus délibérés, que ce banc protège :
 *
 * <ul>
 *   <li>on ne mesure pas une réduction à partir de zéro — sans occurrence
 *       antérieure, il n'y a pas de taux, et prétendre 100 % serait mentir ;</li>
 *   <li>on ne juge pas une fenêtre qui n'est pas écoulée — comparer deux mois
 *       observés à six mois de référence flatte systématiquement le résultat ;</li>
 *   <li>on ne descend pas sous zéro — une CAPA suivie de PLUS de récidives
 *       qu'avant vaut zéro, pas un taux négatif qui se moyennerait ensuite avec
 *       les autres.</li>
 * </ul>
 */
class EffectivenessCalculatorTest {

    static final Instant CLOTURE = Instant.parse("2026-01-15T10:00:00Z");
    static final EffectivenessWindow SIX_MOIS = EffectivenessWindow.ofMonths(6);

    @Test
    void aCapaFollowedByNoRecurrenceIsFullyEffective() {
        CapaEffectiveness result = evaluate(4, 0, CLOTURE.plus(200, ChronoUnit.DAYS));

        assertThat(result.status()).isEqualTo(MeasurementStatus.MEASURED);
        assertThat(result.ratePercent()).isEqualTo(100);
    }

    @Test
    void halfAsManyRecurrencesIsHalfEffective() {
        CapaEffectiveness result = evaluate(4, 2, CLOTURE.plus(200, ChronoUnit.DAYS));

        assertThat(result.ratePercent()).isEqualTo(50);
    }

    @Test
    void aCapaFollowedByAsManyRecurrencesChangedNothing() {
        CapaEffectiveness result = evaluate(3, 3, CLOTURE.plus(200, ChronoUnit.DAYS));

        assertThat(result.ratePercent()).isZero();
        assertThat(result.status()).isEqualTo(MeasurementStatus.MEASURED);
    }

    @Test
    void aCapaFollowedByMoreRecurrencesIsWorthZeroAndNotLess() {
        // Un taux négatif se moyennerait avec les autres et masquerait deux
        // dossiers corrects. Le plancher garde la moyenne lisible ; l'aggravation
        // reste visible dans le décompte des occurrences.
        CapaEffectiveness result = evaluate(2, 9, CLOTURE.plus(200, ChronoUnit.DAYS));

        assertThat(result.ratePercent()).isZero();
        assertThat(result.occurrencesAfter()).isEqualTo(9);
        assertThat(result.aggravated()).isTrue();
    }

    @Test
    void withoutAnyPriorOccurrenceThereIsNothingToMeasure() {
        CapaEffectiveness result = evaluate(0, 0, CLOTURE.plus(200, ChronoUnit.DAYS));

        assertThat(result.status()).isEqualTo(MeasurementStatus.NOT_MEASURABLE);
        assertThat(result.ratePercent()).isNull();
    }

    @Test
    void aWindowStillRunningIsAnnouncedAsSuchWithoutARate() {
        CapaEffectiveness result = evaluate(4, 0, CLOTURE.plus(60, ChronoUnit.DAYS));

        assertThat(result.status()).isEqualTo(MeasurementStatus.IN_OBSERVATION);
        assertThat(result.ratePercent()).isNull();
        // Le décompte partiel, lui, est rendu : « aucune récidive en deux mois »
        // se dit, même si cela ne conclut rien.
        assertThat(result.occurrencesAfter()).isZero();
        assertThat(result.daysObserved()).isEqualTo(60);
        assertThat(result.daysInWindow()).isEqualTo(SIX_MOIS.days());
    }

    @Test
    void anObservationThatEndsExactlyOnTimeIsMeasured() {
        CapaEffectiveness result = evaluate(4, 1, CLOTURE.plus(SIX_MOIS.days(), ChronoUnit.DAYS));

        assertThat(result.status()).isEqualTo(MeasurementStatus.MEASURED);
    }

    @Test
    void theObservedDurationNeverExceedsTheWindow() {
        CapaEffectiveness result = evaluate(4, 1, CLOTURE.plus(400, ChronoUnit.DAYS));

        assertThat(result.daysObserved()).isEqualTo(SIX_MOIS.days());
    }

    @Test
    void aRateIsRoundedToTheNearestPointRatherThanTruncated() {
        // 1 - 1/3 = 66,66… %. Tronquer donnerait 66 et ferait perdre un point à
        // chaque dossier, systématiquement dans le même sens.
        CapaEffectiveness result = evaluate(3, 1, CLOTURE.plus(200, ChronoUnit.DAYS));

        assertThat(result.ratePercent()).isEqualTo(67);
    }

    @Test
    void aCapaThatIsNotClosedHasNothingToMeasure() {
        assertThatThrownBy(() -> EffectivenessCalculator.evaluate(
                null, Instant.now(), SIX_MOIS, 3, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clôturée");
    }

    @Test
    void negativeCountsAreRefusedRatherThanAveraged() {
        assertThatThrownBy(() -> EffectivenessCalculator.evaluate(
                CLOTURE, CLOTURE.plus(200, ChronoUnit.DAYS), SIX_MOIS, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void anObservationCannotStartBeforeTheClosure() {
        // Une horloge qui recule, ou une clôture antidatée : mieux vaut refuser
        // que rendre un nombre de jours négatif dont personne ne verrait qu'il
        // est faux.
        assertThatThrownBy(() -> EffectivenessCalculator.evaluate(
                CLOTURE, CLOTURE.minus(1, ChronoUnit.DAYS), SIX_MOIS, 3, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("antérieure");
    }

    private CapaEffectiveness evaluate(int avant, int apres, Instant maintenant) {
        return EffectivenessCalculator.evaluate(CLOTURE, maintenant, SIX_MOIS, avant, apres);
    }
}
