package com.openlab.qualitos.quality.training;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** Règles de calcul de ceinture (domaine pur — §19.3). */
class BeltLevelTest {

    @ParameterizedTest
    @CsvSource({
            "-50, WHITE",
            "0,   WHITE",
            "99,  WHITE",
            "100, YELLOW",
            "299, YELLOW",
            "300, GREEN",
            "699, GREEN",
            "700, BLACK",
            "5000, BLACK"
    })
    void fromPoints_mapsToExpectedBelt(int points, BeltLevel expected) {
        assertThat(BeltLevel.fromPoints(points)).isEqualTo(expected);
    }

    @Test
    void fromPoints_exactThreshold_isInclusive() {
        assertThat(BeltLevel.fromPoints(BeltLevel.YELLOW.minPoints())).isEqualTo(BeltLevel.YELLOW);
        assertThat(BeltLevel.fromPoints(BeltLevel.GREEN.minPoints())).isEqualTo(BeltLevel.GREEN);
        assertThat(BeltLevel.fromPoints(BeltLevel.BLACK.minPoints())).isEqualTo(BeltLevel.BLACK);
    }

    @ParameterizedTest
    @CsvSource({
            "0,   100",  // 100 - 0 vers YELLOW
            "40,  60",   // 100 - 40 vers YELLOW
            "100, 200",  // 300 - 100 vers GREEN
            "300, 400",  // 700 - 300 vers BLACK
            "700, 0",    // BLACK : palier max
            "9000, 0"
    })
    void pointsToNext_computesRemainingTowardNextBelt(int points, int expected) {
        assertThat(BeltLevel.pointsToNext(points)).isEqualTo(expected);
    }

    @Test
    void pointsToNext_negative_treatedAsZero() {
        assertThat(BeltLevel.pointsToNext(-10)).isEqualTo(100);
    }
}
