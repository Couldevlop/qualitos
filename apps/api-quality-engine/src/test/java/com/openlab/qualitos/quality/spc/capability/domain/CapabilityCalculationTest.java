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
        assertThat(c.constants()).isEqualTo(new SpcConstants(2.33, 0.58, 0, 2.11));
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
}
