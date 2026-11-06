package com.openlab.qualitos.quality.spc.capability.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Le calcul formule par formule, contre les valeurs du classeur de référence.
 */
class CapabilityCalculationTest {

    private static final double EPS = 5e-4;

    @Test
    @DisplayName("chemin doré : la grille du classeur donne les valeurs du classeur, formule par formule")
    void cheminDore() {
        CapabilityCalculation c = CapabilityCalculation.of(SpcFixtures.LSL, SpcFixtures.USL, 5,
                SpcFixtures.goldenGrid());

        assertThat(c.completeSubgroups()).isEqualTo(20);
        assertThat(c.subgroupMeans().get(0)).isCloseTo(498.6, within(1e-9));
        assertThat(c.subgroupRanges().get(0)).isCloseTo(7, within(1e-9));
        assertThat(c.subgroupRanges().get(1)).isCloseTo(16, within(1e-9));
        assertThat(c.sumOfMeans()).isCloseTo(9980.60, within(EPS));
        assertThat(c.grandMean()).isCloseTo(499.030, within(EPS));
        assertThat(c.meanRange()).isCloseTo(10.550, within(EPS));
        assertThat(c.sigma()).isCloseTo(4.5279, within(EPS));
        assertThat(c.cp()).isCloseTo(1.840, within(EPS));
        assertThat(c.cpu()).isCloseTo(1.912, within(EPS));
        assertThat(c.cpl()).isCloseTo(1.769, within(EPS));
        assertThat(c.cpk()).isCloseTo(1.769, within(EPS));
        assertThat(c.meanUcl()).isCloseTo(505.149, within(EPS));
        assertThat(c.meanLcl()).isCloseTo(492.911, within(EPS));
        assertThat(c.rangeUcl()).isCloseTo(22.261, within(EPS));
        assertThat(c.rangeLcl()).isZero();
        assertThat(c.factors())
                .isEqualTo(new ChartFactors(5, DispersionChart.RANGE, 2.33, 0.58, 0, 2.11));
        // Lectures de la maquette : 6 × 4,53 / 50 = 54 % ; moyenne 1,0 sous le milieu = 4 %.
        assertThat(c.dispersionPercent()).isCloseTo(54.33, within(0.01));
        assertThat(c.centerOffset()).isCloseTo(-0.97, within(EPS));
        assertThat(c.centeringPercent()).isCloseTo(3.88, within(EPS));
        assertThat(c.computable()).isTrue();
    }

    @Test
    @DisplayName("un sous-groupe incomplet n'a ni moyenne ni étendue, et ne compte pas dans X̿")
    void sousGroupeIncomplet() {
        List<List<Double>> grille = SpcFixtures.goldenGrid();
        grille.get(2).set(0, null);

        CapabilityCalculation c = CapabilityCalculation.of(SpcFixtures.LSL, SpcFixtures.USL, 5, grille);

        assertThat(c.completeSubgroups()).isEqualTo(19);
        assertThat(c.subgroupMeans().get(0)).isNull();
        assertThat(c.subgroupRanges().get(0)).isNull();
        assertThat(c.grandMean()).isCloseTo((9980.60 - 498.6) / 19, within(1e-6));
    }

    @Test
    @DisplayName("sans sous-groupe complet, rien ne se calcule")
    void grilleVide() {
        CapabilityCalculation c = CapabilityCalculation.of(SpcFixtures.LSL, SpcFixtures.USL, 3,
                SpcFixtures.emptyGrid(3));

        assertThat(c.completeSubgroups()).isZero();
        assertThat(c.subgroupMeans()).hasSize(20).containsOnlyNulls();
        assertThat(c.grandMean()).isNull();
        assertThat(c.cp()).isNull();
        assertThat(c.cpk()).isNull();
        assertThat(c.meanUcl()).isNull();
        assertThat(c.dispersionPercent()).isNull();
        assertThat(c.computable()).isFalse();
    }

    @Test
    @DisplayName("sans dispersion (σ = 0), Cp et Cpk restent nuls au lieu d'être infinis")
    void sansDispersion() {
        List<List<Double>> grille = SpcFixtures.emptyGrid(2);
        grille.get(0).set(0, 500.0);
        grille.get(1).set(0, 500.0);

        CapabilityCalculation c = CapabilityCalculation.of(SpcFixtures.LSL, SpcFixtures.USL, 2, grille);

        assertThat(c.completeSubgroups()).isEqualTo(1);
        assertThat(c.sigma()).isZero();
        assertThat(c.cp()).isNull();
        assertThat(c.cpu()).isNull();
        assertThat(c.cpl()).isNull();
        assertThat(c.cpk()).isNull();
        assertThat(c.computable()).isFalse();
        assertThat(c.meanUcl()).isEqualTo(500.0);
    }

    @Test
    @DisplayName("des limites inversées n'ont pas de lecture en pourcentage de tolérance")
    void limitesInversees() {
        CapabilityCalculation c = CapabilityCalculation.of(525, 475, 5, SpcFixtures.goldenGrid());

        assertThat(c.dispersionPercent()).isNull();
        assertThat(c.centeringPercent()).isNull();
        assertThat(c.cp()).isNegative();
    }

    @Test
    @DisplayName("une grille plus courte que n, ou des lignes nulles, se lisent comme des cellules vides")
    void grilleCourte() {
        List<List<Double>> grille = new ArrayList<>(SpcFixtures.goldenGrid().subList(0, 4));
        grille.set(3, null);
        assertThat(CapabilityCalculation.of(SpcFixtures.LSL, SpcFixtures.USL, 5, grille).completeSubgroups())
                .isZero();
        assertThat(CapabilityCalculation.of(SpcFixtures.LSL, SpcFixtures.USL, 5, null).completeSubgroups())
                .isZero();
        List<List<Double>> courtes = List.of(List.of(500.0), List.of(501.0));
        assertThat(CapabilityCalculation.of(SpcFixtures.LSL, SpcFixtures.USL, 2, courtes).completeSubgroups())
                .isEqualTo(1);
    }

    @Test
    @DisplayName("les listes rendues ne se modifient pas")
    void listesFigees() {
        CapabilityCalculation c = CapabilityCalculation.of(SpcFixtures.LSL, SpcFixtures.USL, 5,
                SpcFixtures.goldenGrid());
        assertThatThrownBy(() -> c.subgroupMeans().add(1.0)).isInstanceOf(UnsupportedOperationException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 9, 10})
    void chaqueTailleAdmiseASesConstantes(int n) {
        SpcConstants k = SpcConstants.forSubgroupSize(n);
        assertThat(k.d2()).isPositive();
        assertThat(k.a2()).isPositive();
        assertThat(k.d4()).isGreaterThan(1);
        assertThat(k.d3()).isGreaterThanOrEqualTo(0);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 11})
    void uneTailleHorsTableEstRefusee(int n) {
        assertThatThrownBy(() -> SpcConstants.forSubgroupSize(n))
                .isInstanceOf(SpcValidationException.class)
                .extracting("field").isEqualTo("subgroupSize");
    }

    /**
     * Le chemin doré de la carte X̄ – S, calculé à la main.
     *
     * <p>Deux sous-groupes de onze mesures, construits pour que l'écart-type
     * soit EXACTEMENT 1 : cinq mesures à (m + 1), cinq à (m − 1), une à m, donc
     * Σ(x − x̄)² = 10 et s = √(10 / (11 − 1)) = 1.
     *
     * <p>D'où, avec c4(11) = 0,975350, A3(11) = 0,927394,
     * B3(11) = 0,321280, B4(11) = 1,678720 (table publiée, cf.
     * {@code StdevConstantsTest}) et s̄ = 1, X̿ = 21 :
     * <ul>
     *   <li>σ̂ = s̄ / c4 = 1 / 0,975350 = 1,0252729 ;</li>
     *   <li>Cp = 12 / (6 × 1,0252729) = 1,9507002 — et comme X̿ = 21 est le
     *       milieu de [15 ; 27], CPU = CPL = Cpk = Cp ;</li>
     *   <li>carte X̄ : 21 ± 0,927394 → [20,0726058 ; 21,9273942] ;</li>
     *   <li>carte S : [0,3212801 ; 1,6787199].</li>
     * </ul>
     */
    @Test
    @DisplayName("chemin doré de la carte X̄ – S : onze mesures par sous-groupe, σ tiré de s̄ / c4")
    void cheminDoreCarteS() {
        List<List<Double>> grille = sousGroupesOnzeMesures(20, 22);

        CapabilityCalculation c = CapabilityCalculation.of(15, 27, 11, grille);

        assertThat(c.factors().chart()).isEqualTo(DispersionChart.STDEV);
        assertThat(c.completeSubgroups()).isEqualTo(2);
        assertThat(c.subgroupMeans()).containsExactly(20.0, 22.0);
        // La composante « ranges » porte ici l'écart-type d'échantillon.
        assertThat(c.subgroupRanges().get(0)).isCloseTo(1.0, within(1e-12));
        assertThat(c.subgroupRanges().get(1)).isCloseTo(1.0, within(1e-12));
        assertThat(c.grandMean()).isCloseTo(21.0, within(1e-12));
        assertThat(c.meanRange()).isCloseTo(1.0, within(1e-12));
        assertThat(c.sigma()).isCloseTo(1.0252729, within(EPS));
        assertThat(c.cp()).isCloseTo(1.9507002, within(EPS));
        assertThat(c.cpu()).isCloseTo(1.9507002, within(EPS));
        assertThat(c.cpl()).isCloseTo(1.9507002, within(EPS));
        assertThat(c.cpk()).isCloseTo(1.9507002, within(EPS));
        assertThat(c.meanUcl()).isCloseTo(21.9273942, within(EPS));
        assertThat(c.meanLcl()).isCloseTo(20.0726058, within(EPS));
        assertThat(c.rangeUcl()).isCloseTo(1.6787199, within(EPS));
        assertThat(c.rangeLcl()).isCloseTo(0.3212801, within(EPS));
        assertThat(c.dispersionPercent()).isCloseTo(51.2636449, within(EPS));
        assertThat(c.centerOffset()).isCloseTo(0, within(1e-12));
        assertThat(c.centeringPercent()).isCloseTo(0, within(1e-12));
        assertThat(c.computable()).isTrue();
    }

    @Test
    @DisplayName("la même grille donne un σ différent selon la carte : l'étendue n'est pas l'écart-type")
    void lEtendueNEstPasLEcartType() {
        // Cinq mesures : étendue 4, écart-type d'échantillon √(10 / 4) = 1,5811388.
        List<List<Double>> grille = List.of(
                List.of(10.0, 10.0), List.of(11.0, 11.0), List.of(12.0, 12.0),
                List.of(13.0, 13.0), List.of(14.0, 14.0));

        CapabilityCalculation r = CapabilityCalculation.of(0, 24, 5, grille);

        assertThat(r.factors().chart()).isEqualTo(DispersionChart.RANGE);
        assertThat(r.meanRange()).isCloseTo(4.0, within(1e-12));
        // σ̂ = R̄ / d2 = 4 / 2,33 ; la carte S aurait donné s̄ / c4 = 1,5811 / 0,94.
        assertThat(r.sigma()).isCloseTo(4 / 2.33, within(1e-9));
        assertThat(ChartFactors.forSubgroupSize(5).dispersionOf(new double[] {10, 11, 12, 13, 14}))
                .isCloseTo(4.0, within(1e-12));
        // Onze mesures de moyenne 12 : Σ(x − x̄)² = 20, donc s = √(20 / 10) = √2.
        assertThat(ChartFactors.forSubgroupSize(11)
                .dispersionOf(new double[] {10, 11, 12, 13, 14, 10, 11, 12, 13, 14, 12}))
                .isCloseTo(Math.sqrt(2), within(1e-12));
    }

    @Test
    @DisplayName("une campagne longue : cent sous-groupes se calculent comme vingt")
    void centSousGroupes() {
        int colonnes = CapabilityCalculation.MAX_SUBGROUPS;
        List<List<Double>> grille = new ArrayList<>();
        for (int r = 0; r < 2; r++) {
            List<Double> ligne = new ArrayList<>();
            for (int c = 0; c < colonnes; c++) {
                ligne.add(r == 0 ? 9.0 : 11.0);
            }
            grille.add(ligne);
        }

        CapabilityCalculation c = CapabilityCalculation.of(5, 15, 2, grille);

        assertThat(colonnes).isEqualTo(100);
        assertThat(c.completeSubgroups()).isEqualTo(100);
        assertThat(c.grandMean()).isCloseTo(10.0, within(1e-12));
        assertThat(c.meanRange()).isCloseTo(2.0, within(1e-12));
        // σ̂ = R̄ / d2 = 2 / 1,13 ; Cp = 10 / (6 σ̂).
        assertThat(c.sigma()).isCloseTo(2 / 1.13, within(1e-9));
        assertThat(c.cp()).isCloseTo(10 / (6 * (2 / 1.13)), within(1e-9));
    }

    /** Deux sous-groupes de onze mesures de moyenne {@code m} et d'écart-type exactement 1. */
    private static List<List<Double>> sousGroupesOnzeMesures(double premiere, double seconde) {
        double[] ecarts = {1, 1, 1, 1, 1, -1, -1, -1, -1, -1, 0};
        List<List<Double>> grille = new ArrayList<>();
        for (double ecart : ecarts) {
            grille.add(new ArrayList<>(List.of(premiere + ecart, seconde + ecart)));
        }
        return grille;
    }
}
