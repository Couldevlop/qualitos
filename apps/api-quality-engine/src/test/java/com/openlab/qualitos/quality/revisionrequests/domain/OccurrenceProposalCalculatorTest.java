package com.openlab.qualitos.quality.revisionrequests.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La table AIAG d'occurrence se lit en défauts par million d'opportunités. Nous ne
 * connaissons pas le volume produit : la plateforme n'a ni ordre de fabrication ni
 * quantité lancée. Compter les NC sur douze mois glissants est donc l'approximation
 * honnête — et elle est nommée comme telle dans la justification affichée à
 * l'utilisateur, qui reste libre de refuser.
 *
 * <p>Une NC ne fait JAMAIS baisser une cote. Un défaut survenu ne peut pas être un
 * argument pour minorer un risque ; seule une CAPA dont l'efficacité a été vérifiée
 * peut proposer une baisse, et c'est un autre chemin.
 */
class OccurrenceProposalCalculatorTest {

    @ParameterizedTest(name = "{0} NC sur 12 mois -> cote {1}")
    @CsvSource({"0, 1", "1, 4", "2, 5", "3, 6", "4, 6", "5, 7", "7, 7",
                "8, 8", "12, 8", "13, 9", "20, 9", "21, 10", "500, 10"})
    void theCountOverTwelveMonthsMapsToARating(int count, int expected) {
        assertThat(OccurrenceProposalCalculator.ratingFor(count)).isEqualTo(expected);
    }

    @Test
    void nothingIsProposedWhenTheComputedRatingDoesNotExceedTheCurrentOne() {
        // Le badge ne doit pas crier pour rien : trois NC donnent 6, et l'item est
        // déjà coté 7. Il n'y a rien à réviser.
        assertThat(OccurrenceProposalCalculator.proposal(7, 3)).isEmpty();
    }

    @Test
    void nothingIsProposedWhenTheRatingIsEqual() {
        assertThat(OccurrenceProposalCalculator.proposal(6, 3)).isEmpty();
    }

    @Test
    void aRiseIsProposedWhenTheHistoryContradictsTheRating() {
        assertThat(OccurrenceProposalCalculator.proposal(4, 3)).hasValue(6);
    }

    @Test
    void aRatingNeverGoesDownOnANonConformity() {
        // Huit NC donnent 8 ; l'item est coté 10. On ne descend pas.
        assertThat(OccurrenceProposalCalculator.proposal(10, 8)).isEmpty();
    }

    @Test
    void aNegativeCountIsTreatedAsZeroRatherThanFailing() {
        assertThat(OccurrenceProposalCalculator.ratingFor(-3)).isEqualTo(1);
    }
}
