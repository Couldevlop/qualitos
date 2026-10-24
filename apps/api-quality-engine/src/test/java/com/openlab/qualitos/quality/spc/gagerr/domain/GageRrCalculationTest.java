package com.openlab.qualitos.quality.spc.gagerr.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Le calcul R&amp;R contre des valeurs calculées indépendamment (numpy, mpmath)
 * sur la grille du classeur et sur des grilles construites pour chaque branche.
 */
class GageRrCalculationTest {

    static final double E = 1e-9;

    static GageRrCalculation classeur() {
        return GageRrCalculation.of(GageRrFixtures.LSL, GageRrFixtures.USL, 5.15, GageRrFixtures.workbookGrid());
    }

    @Test
    @DisplayName("étape 1 : moyennes et étendues par opérateur, X̄diff, R̄̄, Rp, UCL R")
    void etape1() {
        GageRrCalculation c = classeur();
        assertThat(c.operators()).isEqualTo(3);
        assertThat(c.parts()).isEqualTo(10);
        assertThat(c.trials()).isEqualTo(3);
        assertThat(c.tolerance()).isCloseTo(0.2, within(E));
        assertThat(c.operatorMeans().get(0)).isCloseTo(10.010166666666667, within(E));
        assertThat(c.operatorMeans().get(1)).isCloseTo(10.01843333333333, within(E));
        assertThat(c.operatorMeans().get(2)).isCloseTo(10.008, within(E));
        assertThat(c.operatorRanges().get(0)).isCloseTo(0.0117, within(E));
        assertThat(c.operatorRanges().get(1)).isCloseTo(0.0154, within(E));
        assertThat(c.operatorRanges().get(2)).isCloseTo(0.0136, within(E));
        assertThat(c.xDiff()).isCloseTo(0.010433333333331518, within(E));
        assertThat(c.meanRange()).isCloseTo(0.013566666666666845, within(E));
        assertThat(c.partRange()).isCloseTo(0.14222222222222314, within(E));
        assertThat(c.rangeUcl()).isCloseTo(0.03500200000000046, within(E));
        assertThat(c.maxRange()).isCloseTo(0.028, within(E));
        assertThat(c.rangesBeyondLimit()).isZero();
        assertThat(c.grandMean()).isCloseTo(10.0122, within(E));
        // Pièce 1, opérateur A : moyenne (9,955 + 9,951 + 9,94) / 3, étendue 0,015.
        assertThat(c.cellMeans().get(0).get(0)).isCloseTo(9.948666666666666, within(E));
        assertThat(c.cellRanges().get(0).get(0)).isCloseTo(0.015, within(E));
        assertThat(c.partMeans()).hasSize(10);
        assertThat(c.partMeans().get(0)).isCloseTo(9.944555555555556, within(E));
        // Ligne « Moyenne » du classeur, colonne Essai 1 de l'opérateur A.
        assertThat(c.trialMeans().get(0).get(0)).isCloseTo(10.0103, within(E));
        assertThat(c.trialMeans()).hasSize(3).allSatisfy(t -> assertThat(t).hasSize(3));
    }

    @Test
    @DisplayName("étape 2 : constantes lues pour r = 3, k = 3, n = 10 à 5,15 σ")
    void etape2() {
        GageRrConstants k = classeur().constants();
        assertThat(k.k1()).isCloseTo(3.05, within(E));
        assertThat(k.k2()).isCloseTo(2.70, within(E));
        assertThat(k.k3()).isCloseTo(1.62, within(E));
        assertThat(k.d4()).isCloseTo(2.58, within(E));
    }

    @Test
    @DisplayName("étapes 3 et 4 : EV, AV, GRR, PV, TV, pourcentages, ndc et verdict du classeur")
    void etapes3et4() {
        GageRrCalculation c = classeur();
        assertThat(c.ev().value()).isCloseTo(0.04137833333333388, within(E));
        assertThat(c.av().value()).isCloseTo(0.02713810391961042, within(E));
        assertThat(c.grr().value()).isCloseTo(0.049483766568401644, within(E));
        assertThat(c.pv().value()).isCloseTo(0.23040000000000152, within(E));
        assertThat(c.tv().value()).isCloseTo(0.2356539903201233, within(E));

        assertThat(c.ev().percentOfTotal()).isCloseTo(0.1755893599642579, within(E));
        assertThat(c.av().percentOfTotal()).isCloseTo(0.11516080794025496, within(E));
        assertThat(c.grr().percentOfTotal()).isCloseTo(0.20998484473435228, within(E));
        assertThat(c.pv().percentOfTotal()).isCloseTo(0.9777046409738935, within(E));
        assertThat(c.tv().percentOfTotal()).isCloseTo(1.0, within(E));

        assertThat(c.ev().percentContribution()).isCloseTo(0.030831623332657734, within(E));
        assertThat(c.av().percentContribution()).isCloseTo(0.01326201168545229, within(E));
        assertThat(c.pv().percentContribution()).isCloseTo(0.9559063649818901, within(E));
        // Les contributions s'additionnent : EV + AV + PV = 100 %.
        assertThat(c.ev().percentContribution() + c.av().percentContribution() + c.pv().percentContribution())
                .isCloseTo(1.0, within(E));

        assertThat(c.grr().percentTolerance()).isCloseTo(0.24741883284200822, within(E));
        assertThat(c.pv().percentTolerance()).isCloseTo(1.1520000000000075, within(E));

        assertThat(c.ndc()).isEqualTo(6);
        assertThat(c.ndcSufficient()).isTrue();
        assertThat(c.decision()).isEqualTo(GageRrDecision.CONDITIONAL);
        assertThat(c.repeatabilityDominant()).isTrue();
        assertThat(c.computable()).isTrue();
        assertThat(c.multiplier()).isEqualTo(5.15);
    }

    @Test
    @DisplayName("ANOVA du classeur : interaction non significative (p = 0,358), regroupée")
    void anovaDuClasseur() {
        GageRrAnova a = classeur().anova();
        GageRrAnova.AnovaRow pieces = a.row(GageRrAnova.Source.PART);
        GageRrAnova.AnovaRow operateurs = a.row(GageRrAnova.Source.OPERATOR);
        GageRrAnova.AnovaRow interaction = a.row(GageRrAnova.Source.INTERACTION);
        GageRrAnova.AnovaRow repetabilite = a.row(GageRrAnova.Source.REPEATABILITY);
        GageRrAnova.AnovaRow total = a.row(GageRrAnova.Source.TOTAL);

        assertThat(pieces.degreesOfFreedom()).isEqualTo(9);
        assertThat(operateurs.degreesOfFreedom()).isEqualTo(2);
        assertThat(interaction.degreesOfFreedom()).isEqualTo(18);
        assertThat(repetabilite.degreesOfFreedom()).isEqualTo(60);
        assertThat(total.degreesOfFreedom()).isEqualTo(89);

        assertThat(pieces.sumOfSquares()).isCloseTo(0.1511726222222247, within(1e-12));
        assertThat(operateurs.sumOfSquares()).isCloseTo(0.0018188666666665457, within(1e-12));
        assertThat(interaction.sumOfSquares()).isCloseTo(0.0011515777777759617, within(1e-12));
        assertThat(repetabilite.sumOfSquares()).isCloseTo(0.0034333333333333915, within(1e-12));
        assertThat(total.sumOfSquares()).isCloseTo(0.1575764000000006, within(1e-12));

        assertThat(pieces.meanSquare()).isCloseTo(0.01679695802469163, within(1e-12));
        assertThat(operateurs.meanSquare()).isCloseTo(0.0009094333333332728, within(1e-12));
        assertThat(interaction.meanSquare()).isCloseTo(6.397654320977565e-05, within(1e-12));
        assertThat(repetabilite.meanSquare()).isCloseTo(5.722222222222319e-05, within(1e-12));
        assertThat(total.meanSquare()).isNull();

        assertThat(pieces.f()).isCloseTo(262.5486964748206, within(1e-6));
        assertThat(operateurs.f()).isCloseTo(14.215105845142176, within(1e-6));
        assertThat(interaction.f()).isCloseTo(1.1180366774523711, within(1e-6));
        assertThat(pieces.p()).isCloseTo(3.192045013651395e-17, within(1e-20));
        assertThat(operateurs.p()).isCloseTo(0.00019780923897179872, within(1e-10));
        assertThat(interaction.p()).isCloseTo(0.3583858414026355, within(1e-8));
        assertThat(repetabilite.f()).isNull();
        assertThat(total.p()).isNull();

        assertThat(a.interactionPooled()).isTrue();
        assertThat(a.pooledTable()).hasSize(4);
        GageRrAnova.AnovaRow piecesRegroupees = a.pooledTable().get(0);
        GageRrAnova.AnovaRow operateursRegroupes = a.pooledTable().get(1);
        GageRrAnova.AnovaRow repetabiliteRegroupee = a.pooledTable().get(2);
        assertThat(repetabiliteRegroupee.degreesOfFreedom()).isEqualTo(78);
        assertThat(repetabiliteRegroupee.meanSquare()).isCloseTo(5.8780911680889144e-05, within(1e-12));
        assertThat(piecesRegroupees.f()).isCloseTo(285.75531655376926, within(1e-6));
        assertThat(operateursRegroupes.f()).isCloseTo(15.471575845410849, within(1e-6));
        assertThat(piecesRegroupees.p()).isCloseTo(6.761617554278048e-56, within(1e-60));
        assertThat(operateursRegroupes.p()).isCloseTo(2.192259876033559e-06, within(1e-12));

        GageRrAnova.Variance v = a.variance();
        assertThat(v.repeatability()).isCloseTo(5.8780911680889144e-05, within(1e-12));
        assertThat(v.operator()).isCloseTo(2.8355080721746123e-05, within(1e-12));
        assertThat(v.interaction()).isZero();
        assertThat(v.reproducibility()).isCloseTo(2.8355080721746123e-05, within(1e-12));
        assertThat(v.part()).isCloseTo(0.0018597974570011937, within(1e-12));
        assertThat(v.total()).isCloseTo(0.001946933449403829, within(1e-12));
        assertThat(v.grr()).isCloseTo(v.repeatability() + v.reproducibility(), within(1e-15));

        assertThat(a.ev().value()).isCloseTo(0.03948438590197881, within(E));
        assertThat(a.av().value()).isCloseTo(0.027423486803149443, within(E));
        assertThat(a.operator().value()).isCloseTo(0.027423486803149443, within(E));
        assertThat(a.interaction().value()).isZero();
        assertThat(a.grr().value()).isCloseTo(0.048073530747167866, within(E));
        assertThat(a.pv().value()).isCloseTo(0.22209565068527154, within(E));
        assertThat(a.tv().value()).isCloseTo(0.22723895443302203, within(E));
        assertThat(a.grr().percentOfTotal()).isCloseTo(0.21155497246110322, within(E));
        assertThat(a.grr().percentContribution()).isCloseTo(0.044755506373018145, within(E));
        assertThat(a.grr().percentTolerance()).isCloseTo(0.24036765373583932, within(E));
        assertThat(a.ndc()).isEqualTo(6);
        assertThat(a.decision()).isEqualTo(GageRrDecision.CONDITIONAL);
    }

    @Test
    @DisplayName("à 6 σ, les constantes et les dispersions sont mises à l'échelle, pas les pourcentages")
    void sixSigma() {
        GageRrCalculation c515 = classeur();
        GageRrCalculation c6 = GageRrCalculation.of(GageRrFixtures.LSL, GageRrFixtures.USL, 6,
                GageRrFixtures.workbookGrid());
        double echelle = 6 / 5.15;
        assertThat(c6.constants().k1()).isCloseTo(3.05 * echelle, within(E));
        assertThat(c6.constants().d4()).isEqualTo(2.58);
        assertThat(c6.ev().value()).isCloseTo(c515.ev().value() * echelle, within(E));
        assertThat(c6.av().value()).isCloseTo(c515.av().value() * echelle, within(E));
        assertThat(c6.tv().value()).isCloseTo(c515.tv().value() * echelle, within(E));
        assertThat(c6.grr().percentOfTotal()).isCloseTo(c515.grr().percentOfTotal(), within(E));
        assertThat(c6.grr().percentTolerance()).isCloseTo(c515.grr().percentTolerance() * echelle, within(E));
        assertThat(c6.anova().grr().value()).isCloseTo(c515.anova().grr().value() * echelle, within(E));
        assertThat(c6.anova().grr().percentOfTotal()).isCloseTo(c515.anova().grr().percentOfTotal(), within(E));
        assertThat(c6.ndc()).isEqualTo(c515.ndc());
    }

    @Test
    @DisplayName("interaction forte : non regroupée, F des facteurs rapportés à l'interaction, AV porte l'interaction")
    void interactionForte() {
        List<List<List<Double>>> g = GageRrFixtures.grid(new double[][][]{
                {{1.0, 1.01}, {2.0, 2.01}, {1.5, 1.52}},
                {{2.0, 2.02}, {1.0, 1.0}, {1.6, 1.61}}});
        GageRrCalculation c = GageRrCalculation.of(0, 3, 5.15, g);

        assertThat(c.xDiff()).isCloseTo(0.03166666666666673, within(E));
        assertThat(c.meanRange()).isCloseTo(0.011666666666666641, within(E));
        assertThat(c.ev().value()).isCloseTo(0.05319999999999988, within(E));
        assertThat(c.av().value()).isCloseTo(0.11352444792985268, within(E));
        assertThat(c.pv().value()).isCloseTo(0.14850000000000044, within(E));
        assertThat(c.tv().value()).isCloseTo(0.19434580077217503, within(E));
        assertThat(c.ndc()).isEqualTo(1);
        assertThat(c.ndcSufficient()).isFalse();
        assertThat(c.decision()).isEqualTo(GageRrDecision.NOT_ACCEPTABLE);
        assertThat(c.repeatabilityDominant()).isFalse();

        GageRrAnova a = c.anova();
        assertThat(a.interactionPooled()).isFalse();
        assertThat(a.pooledTable()).isNull();
        assertThat(a.row(GageRrAnova.Source.INTERACTION).f()).isCloseTo(11051.27272727275, within(1e-6));
        assertThat(a.row(GageRrAnova.Source.INTERACTION).p()).isCloseTo(1.9988179400586884e-11, within(1e-15));
        assertThat(a.row(GageRrAnova.Source.PART).f()).isCloseTo(0.003652397091244147, within(1e-12));
        assertThat(a.row(GageRrAnova.Source.PART).p()).isCloseTo(0.9963608943675825, within(1e-10));
        assertThat(a.row(GageRrAnova.Source.OPERATOR).p()).isCloseTo(0.961495283696702, within(1e-10));
        assertThat(a.variance().interaction()).isCloseTo(0.5064708333333333, within(1e-12));
        assertThat(a.variance().operator()).isZero();
        assertThat(a.variance().part()).isZero();
        assertThat(a.av().value()).isCloseTo(3.665088358700692, within(1e-9));
        assertThat(a.interaction().value()).isCloseTo(3.665088358700692, within(1e-9));
        assertThat(a.pv().value()).isZero();
        assertThat(a.ndc()).isZero();
        assertThat(a.decision()).isEqualTo(GageRrDecision.NOT_ACCEPTABLE);
    }

    @Test
    @DisplayName("une étendue au-dessus de UCL R est comptée")
    void etendueHorsLimite() {
        List<List<List<Double>>> g = GageRrFixtures.grid(new double[][][]{
                {{10.0, 10.0}, {11.0, 11.0}, {12.0, 12.0}},
                {{10.0, 10.9}, {11.0, 11.0}, {12.0, 12.0}}});
        GageRrCalculation c = GageRrCalculation.of(9, 13, 5.15, g);
        assertThat(c.rangeUcl()).isCloseTo(0.49050000000000016, within(E));
        assertThat(c.rangesBeyondLimit()).isEqualTo(1);
        assertThat(c.ndc()).isEqualTo(8);
        assertThat(c.anova().interactionPooled()).isTrue();
        assertThat(c.anova().row(GageRrAnova.Source.INTERACTION).p()).isCloseTo(0.421875000000006, within(1e-9));
        assertThat(c.anova().ndc()).isEqualTo(4);
        assertThat(c.anova().pv().value()).isCloseTo(4.533755341877196, within(1e-9));
    }

    @Test
    @DisplayName("des opérateurs identiques : AV ramené à 0, la répétabilité domine")
    void avRameneAZero() {
        List<List<List<Double>>> g = GageRrFixtures.grid(new double[][][]{
                {{1.0, 1.1}, {2.0, 2.1}},
                {{1.0, 1.1}, {2.0, 2.1}}});
        GageRrCalculation c = GageRrCalculation.of(0, 3, 5.15, g);
        assertThat(c.xDiff()).isCloseTo(0, within(E));
        assertThat(c.av().value()).isZero();
        assertThat(c.repeatabilityDominant()).isTrue();
    }

    @Test
    @DisplayName("sans répétabilité ni interaction : F de l'interaction indéfini, regroupement, F nuls")
    void sansRepetabiliteNiInteraction() {
        List<List<List<Double>>> g = GageRrFixtures.grid(new double[][][]{
                {{1.0, 1.0}, {2.0, 2.0}},
                {{1.1, 1.1}, {2.1, 2.1}}});
        GageRrCalculation c = GageRrCalculation.of(0, 3, 5.15, g);
        GageRrAnova a = c.anova();
        assertThat(a.row(GageRrAnova.Source.INTERACTION).f()).isNull();
        assertThat(a.row(GageRrAnova.Source.INTERACTION).p()).isNull();
        assertThat(a.row(GageRrAnova.Source.PART).f()).isNull();
        assertThat(a.interactionPooled()).isTrue();
        assertThat(a.pooledTable().get(0).f()).isNull();
        assertThat(a.variance().repeatability()).isZero();
        assertThat(c.ev().value()).isZero();
        assertThat(c.repeatabilityDominant()).isFalse();
    }

    @Test
    @DisplayName("sans répétabilité mais avec interaction : pas de regroupement")
    void sansRepetabiliteAvecInteraction() {
        List<List<List<Double>>> g = GageRrFixtures.grid(new double[][][]{
                {{1.0, 1.0}, {2.0, 2.0}},
                {{2.0, 2.0}, {1.0, 1.0}}});
        GageRrAnova a = GageRrCalculation.of(0, 3, 5.15, g).anova();
        assertThat(a.row(GageRrAnova.Source.INTERACTION).p()).isNull();
        assertThat(a.interactionPooled()).isFalse();
        assertThat(a.variance().interaction()).isCloseTo(1.0, within(E));
    }

    @Test
    @DisplayName("aucune dispersion : rien ne se calcule, ndc 0, pourcentages à 0")
    void aucuneDispersion() {
        List<List<List<Double>>> g = GageRrFixtures.grid(new double[][][]{
                {{5.0, 5.0}, {5.0, 5.0}},
                {{5.0, 5.0}, {5.0, 5.0}}});
        GageRrCalculation c = GageRrCalculation.of(0, 0, 5.15, g);
        assertThat(c.computable()).isFalse();
        assertThat(c.ndc()).isZero();
        assertThat(c.grr().percentOfTotal()).isZero();
        assertThat(c.grr().percentTolerance()).isZero();
        assertThat(c.decision()).isEqualTo(GageRrDecision.ACCEPTABLE);
        assertThat(c.anova().ndc()).isZero();
        assertThat(c.anova().tv().value()).isZero();
    }

    @Test
    void ndcSuitLaRegleDuClasseur() {
        assertThat(GageRrCalculation.ndc(1, 0)).isZero();
        assertThat(GageRrCalculation.ndc(0.2304, 0.04948)).isEqualTo(6);
        assertThat(GageRrCalculation.ndc(1.0, 0.3)).isEqualTo(4);
    }
}
