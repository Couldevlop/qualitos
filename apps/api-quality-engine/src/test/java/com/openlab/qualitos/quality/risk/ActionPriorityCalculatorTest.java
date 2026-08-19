package com.openlab.qualitos.quality.risk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Le RPN donne le même 120 pour (10, 4, 3) — une défaillance grave — et pour
 * (4, 10, 3) — une défaillance fréquente et bénigne. C'est précisément ce que
 * l'AIAG-VDA a corrigé en remplaçant le produit des trois notes par une lecture
 * séparée de chacune.
 *
 * <p>La matrice implémentée ici lit les trois notes par bandes. Elle suit l'esprit
 * du manuel AIAG-VDA sans en recopier la table — le manuel est sous droits, et une
 * table recopiée de mémoire serait fausse quelque part sans qu'on sache où. Les
 * bandes sont documentées, testées et modifiables ; c'est plus honnête qu'une
 * fidélité prétendue.
 */
class ActionPriorityCalculatorTest {

    @Test
    void theRpnCannotSeparateTwoVeryDifferentRisksButTheApCan() {
        assertThat(10 * 4 * 3).isEqualTo(4 * 10 * 3);

        assertThat(ActionPriorityCalculator.of(10, 4, 3)).isEqualTo(ActionPriority.HIGH);
        assertThat(ActionPriorityCalculator.of(4, 10, 3)).isEqualTo(ActionPriority.MEDIUM);
    }

    @ParameterizedTest(name = "S={0} O={1} D={2} -> {3}")
    @CsvSource({
            // sévérité haute (9-10) : jamais LOW, quelle que soit la détection
            "10, 10, 10, HIGH", "10, 6, 1, HIGH", "9, 3, 1, HIGH",
            "10, 1, 1, MEDIUM", "9, 2, 3, MEDIUM", "9, 1, 7, HIGH",
            // sévérité moyenne (5-8)
            "8, 8, 1, HIGH", "7, 4, 4, MEDIUM", "6, 3, 8, HIGH",
            "5, 2, 2, LOW", "5, 1, 5, MEDIUM", "8, 2, 7, MEDIUM",
            // sévérité basse (1-4)
            "4, 10, 10, MEDIUM", "3, 4, 3, LOW", "1, 1, 1, LOW",
            "4, 5, 9, MEDIUM", "2, 2, 8, MEDIUM"
    })
    void theMatrixReadsTheThreeRatingsSeparately(int s, int o, int d, ActionPriority expected) {
        assertThat(ActionPriorityCalculator.of(s, o, d)).isEqualTo(expected);
    }

    @Test
    void anUnratedItemHasNoActionPriority() {
        // Un FmeaItem fraîchement instancié a S=O=D=0. recomputeRpn() tourne dans
        // @PrePersist : lever ici casserait toute création d'item non encore coté.
        FmeaItem item = new FmeaItem();

        item.recomputeRpn();

        assertThat(item.getActionPriority()).isNull();
    }

    @ParameterizedTest
    @CsvSource({"0, 5, 5", "11, 5, 5", "5, 0, 5", "5, 5, 11"})
    void aRatingOutsideOneToTenIsRefused(int s, int o, int d) {
        assertThatThrownBy(() -> ActionPriorityCalculator.of(s, o, d))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
