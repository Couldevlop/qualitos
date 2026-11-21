package com.openlab.qualitos.quality.objective;

import com.openlab.qualitos.quality.kpi.KpiDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("La règle d'un objectif qualité (ADR 0133)")
class ObjectiveEvaluatorTest {

    /** Taux de NC client : on veut descendre. Cible 500, alerte 600, critique 800. */
    private static QualityObjective baisse(int warningPeriods, int criticalPeriods,
                                           int recoveryPeriods) {
        QualityObjective o = new QualityObjective();
        o.setTitle("Taux de NC client");
        o.setTargetValue(bd("500"));
        o.setThresholdWarning(bd("600"));
        o.setThresholdCritical(bd("800"));
        o.setWarningPeriods(warningPeriods);
        o.setCriticalPeriods(criticalPeriods);
        o.setRecoveryPeriods(recoveryPeriods);
        o.setPeriodMonths(1);
        return o;
    }

    /** Livraison à l'heure : on veut monter. Cible 95, alerte 93, critique 90. */
    private static QualityObjective hausse(int warningPeriods, int criticalPeriods,
                                           int recoveryPeriods) {
        QualityObjective o = new QualityObjective();
        o.setTitle("Livraison à l'heure");
        o.setTargetValue(bd("95"));
        o.setThresholdWarning(bd("93"));
        o.setThresholdCritical(bd("90"));
        o.setWarningPeriods(warningPeriods);
        o.setCriticalPeriods(criticalPeriods);
        o.setRecoveryPeriods(recoveryPeriods);
        o.setPeriodMonths(1);
        return o;
    }

    private static BigDecimal bd(String s) { return new BigDecimal(s); }

    private static List<BigDecimal> serie(String... valeurs) {
        return Arrays.stream(valeurs).map(ObjectiveEvaluatorTest::bd).toList();
    }

    @Nested
    @DisplayName("Sans mesure")
    class SansMesure {

        @Test
        void aucuneMesure_rienAApprecier() {
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 1), KpiDirection.LOWER_IS_BETTER, List.of(), null);
            assertThat(a.status()).isEqualTo(ObjectiveStatus.NOT_EVALUATED);
            assertThat(a.observedValue()).isNull();
            assertThat(a.triggeredLevel()).isNull();
            assertThat(a.periodsConsidered()).isZero();
        }

        @Test
        void listeNulle_traiteeCommeVide() {
            assertThat(ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 1), KpiDirection.LOWER_IS_BETTER, null, null).status())
                    .isEqualTo(ObjectiveStatus.NOT_EVALUATED);
        }

        @Test
        void valeursNulles_ignorees_etSiToutEstNulRienAApprecier() {
            assertThat(ObjectiveEvaluator.evaluate(baisse(2, 1, 1),
                    KpiDirection.LOWER_IS_BETTER, Arrays.asList(null, null), null).status())
                    .isEqualTo(ObjectiveStatus.NOT_EVALUATED);
        }

        @Test
        void sansObjectif_leRefusEstExplicite() {
            assertThatThrownBy(() -> ObjectiveEvaluator.evaluate(
                    null, KpiDirection.LOWER_IS_BETTER, serie("1"), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("objectif");
        }

        @Test
        void sansSensDAmelioration_leRefusDitPourquoi() {
            assertThatThrownBy(() -> ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 1), null, serie("1"), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dépasser le seuil");
        }
    }

    @Nested
    @DisplayName("Un indicateur qu'on veut faire BAISSER")
    class Baisse {

        @Test
        void sousLaCible_atteint() {
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 1), KpiDirection.LOWER_IS_BETTER, serie("480", "420"), null);
            assertThat(a.status()).isEqualTo(ObjectiveStatus.ON_TARGET);
            assertThat(a.onTargetPeriods()).isEqualTo(2);
            assertThat(a.triggeredLevel()).isNull();
        }

        @Test
        void surLaCibleExactement_atteint() {
            // « ≤ 500 ppm » est atteint à 500 pile : un objectif qu'on manque en
            // l'atteignant serait incompréhensible devant une revue.
            assertThat(ObjectiveEvaluator.evaluate(baisse(2, 1, 1),
                    KpiDirection.LOWER_IS_BETTER, serie("500"), null).status())
                    .isEqualTo(ObjectiveStatus.ON_TARGET);
        }

        @Test
        void horsCibleSansFranchirLAlerte_enAlerteSansDeclencher() {
            // 550 : au-dessus de la cible, pas au-dessus du seuil d'alerte.
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 1), KpiDirection.LOWER_IS_BETTER, serie("550", "550"), null);
            assertThat(a.status()).isEqualTo(ObjectiveStatus.WARNING);
            assertThat(a.warningBreaches()).isZero();
            assertThat(a.triggeredLevel()).isNull();
        }

        @Test
        void surLeSeuilExactement_nEstPasUnDepassement() {
            // « dépasse le seuil » se lit strictement, comme le dit la maquette.
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(1, 1, 1), KpiDirection.LOWER_IS_BETTER, serie("600"), null);
            assertThat(a.warningBreaches()).isZero();
            assertThat(a.triggeredLevel()).isNull();
        }

        @Test
        void uneSeulePeriodeAuDelaDeLAlerte_neDeclenchePasEncore() {
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 1), KpiDirection.LOWER_IS_BETTER, serie("480", "650"), null);
            assertThat(a.warningBreaches()).isEqualTo(1);
            assertThat(a.triggeredLevel()).isNull();
            assertThat(a.status()).isEqualTo(ObjectiveStatus.WARNING);
        }

        @Test
        void deuxPeriodesConsecutivesAuDelaDeLAlerte_proposent() {
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 1), KpiDirection.LOWER_IS_BETTER,
                    serie("480", "650", "660"), null);
            assertThat(a.warningBreaches()).isEqualTo(2);
            assertThat(a.triggeredLevel()).isEqualTo(ObjectiveLevel.WARNING);
        }

        @Test
        void deuxDepassementsNonConsecutifs_neDeclenchentPas() {
            // Les comptages partent de la FIN : 650, 480, 660 ne fait qu'une
            // période consécutive, pas deux.
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 1), KpiDirection.LOWER_IS_BETTER,
                    serie("650", "480", "660"), null);
            assertThat(a.warningBreaches()).isEqualTo(1);
            assertThat(a.triggeredLevel()).isNull();
        }

        @Test
        void auDelaDuCritique_critiqueDesLaPremierePeriode() {
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 1), KpiDirection.LOWER_IS_BETTER, serie("480", "900"), null);
            assertThat(a.status()).isEqualTo(ObjectiveStatus.CRITICAL);
            assertThat(a.criticalBreaches()).isEqualTo(1);
            assertThat(a.triggeredLevel()).isEqualTo(ObjectiveLevel.CRITICAL);
        }

        @Test
        void leCritiqueLEmporteSurLAlerte() {
            // Les deux règles déclenchent ; c'est le critique qui est annoncé.
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 1), KpiDirection.LOWER_IS_BETTER, serie("900", "950"), null);
            assertThat(a.warningBreaches()).isEqualTo(2);
            assertThat(a.criticalBreaches()).isEqualTo(2);
            assertThat(a.triggeredLevel()).isEqualTo(ObjectiveLevel.CRITICAL);
        }

        @Test
        void leCritiquePeutExigerPlusieursPeriodes() {
            QualityObjective o = baisse(2, 3, 1);
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    o, KpiDirection.LOWER_IS_BETTER, serie("900", "950"), null);
            // Critique à l'affichage dès le premier dépassement, mais la règle
            // n'a pas encore ses trois périodes : elle propose au titre de
            // l'alerte, qui, elle, est remplie.
            assertThat(a.status()).isEqualTo(ObjectiveStatus.CRITICAL);
            assertThat(a.triggeredLevel()).isEqualTo(ObjectiveLevel.WARNING);
        }

        @Test
        void lEcartEstCalculeEnValeurEtEnPourcentage() {
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 1), KpiDirection.LOWER_IS_BETTER, serie("720"), null);
            assertThat(a.deviationAbsolute()).isEqualByComparingTo("220");
            assertThat(a.deviationPercent()).isEqualByComparingTo("44");
        }

        @Test
        void cibleAZero_pasDePourcentage() {
            // Diviser par zéro ne dirait rien : on rend l'écart absolu seul.
            QualityObjective o = baisse(2, 1, 1);
            o.setTargetValue(BigDecimal.ZERO);
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    o, KpiDirection.LOWER_IS_BETTER, serie("5"), null);
            assertThat(a.deviationAbsolute()).isEqualByComparingTo("5");
            assertThat(a.deviationPercent()).isNull();
        }
    }

    @Nested
    @DisplayName("Un indicateur qu'on veut faire MONTER")
    class Hausse {

        @Test
        void auDessusDeLaCible_atteint() {
            assertThat(ObjectiveEvaluator.evaluate(hausse(2, 1, 1),
                    KpiDirection.HIGHER_IS_BETTER, serie("97.8"), null).status())
                    .isEqualTo(ObjectiveStatus.ON_TARGET);
        }

        @Test
        void sousLaCibleSansFranchirLAlerte_enAlerte() {
            // 94 : sous la cible 95, au-dessus du seuil d'alerte 93.
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    hausse(2, 1, 1), KpiDirection.HIGHER_IS_BETTER, serie("94", "94"), null);
            assertThat(a.status()).isEqualTo(ObjectiveStatus.WARNING);
            assertThat(a.warningBreaches()).isZero();
        }

        @Test
        void sousLeSeuilDAlerte_leSensEstInverse() {
            // Dépasser un seuil, ici, c'est passer EN DESSOUS.
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    hausse(2, 1, 1), KpiDirection.HIGHER_IS_BETTER, serie("92", "91.5"), null);
            assertThat(a.warningBreaches()).isEqualTo(2);
            assertThat(a.triggeredLevel()).isEqualTo(ObjectiveLevel.WARNING);
        }

        @Test
        void sousLeSeuilCritique_critique() {
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    hausse(2, 1, 1), KpiDirection.HIGHER_IS_BETTER, serie("88"), null);
            assertThat(a.status()).isEqualTo(ObjectiveStatus.CRITICAL);
            assertThat(a.triggeredLevel()).isEqualTo(ObjectiveLevel.CRITICAL);
        }

        @Test
        void lEcartEstNegatifQuandOnEstEnDessous() {
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    hausse(2, 1, 1), KpiDirection.HIGHER_IS_BETTER, serie("93.4"), null);
            assertThat(a.deviationAbsolute()).isEqualByComparingTo("-1.6");
        }
    }

    @Nested
    @DisplayName("Le retour sous la cible, et la mémoire de l'écart")
    class Retour {

        @Test
        void sansCapaOuverte_leRetourAuVertSuffitAAfficherAtteint() {
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 3), KpiDirection.LOWER_IS_BETTER,
                    serie("900", "900", "480"), null);
            assertThat(a.status()).isEqualTo(ObjectiveStatus.ON_TARGET);
            assertThat(a.heldByOpenCapa()).isFalse();
        }

        @Test
        void capaOuverte_unSeulMoisAuVertNeSuffitPas() {
            // L'objectif exige trois périodes sous la cible ; il n'en a qu'une.
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 3), KpiDirection.LOWER_IS_BETTER,
                    serie("900", "900", "480"), ObjectiveLevel.CRITICAL);
            assertThat(a.onTargetPeriods()).isEqualTo(1);
            assertThat(a.recoveryAchieved()).isFalse();
            assertThat(a.heldByOpenCapa()).isTrue();
            assertThat(a.status()).isEqualTo(ObjectiveStatus.CRITICAL);
        }

        @Test
        void capaOuverte_laDureeExigeeAtteinte_lObjectifRepasseAtteint() {
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 3), KpiDirection.LOWER_IS_BETTER,
                    serie("900", "480", "470", "460"), ObjectiveLevel.CRITICAL);
            assertThat(a.onTargetPeriods()).isEqualTo(3);
            assertThat(a.recoveryAchieved()).isTrue();
            assertThat(a.heldByOpenCapa()).isFalse();
            assertThat(a.status()).isEqualTo(ObjectiveStatus.ON_TARGET);
        }

        @Test
        void capaEnAlerteOuverte_leMaintienNAggravePasUnEtatDejaPire() {
            // Le KPI est critique aujourd'hui ; une CAPA d'alerte ouverte ne
            // doit pas le faire paraître moins grave qu'il n'est.
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 2), KpiDirection.LOWER_IS_BETTER,
                    serie("900"), ObjectiveLevel.WARNING);
            assertThat(a.status()).isEqualTo(ObjectiveStatus.CRITICAL);
            assertThat(a.heldByOpenCapa()).isTrue();
        }

        @Test
        void leRetourSeCompteDepuisLaFin_uneRechuteRemetLeCompteurAZero() {
            ObjectiveAssessment a = ObjectiveEvaluator.evaluate(
                    baisse(2, 1, 3), KpiDirection.LOWER_IS_BETTER,
                    serie("480", "470", "900"), ObjectiveLevel.CRITICAL);
            assertThat(a.onTargetPeriods()).isZero();
            assertThat(a.recoveryAchieved()).isFalse();
        }
    }
}
