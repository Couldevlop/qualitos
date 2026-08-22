package com.openlab.qualitos.quality.revisionrequests.domain;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rattacher une NC au mauvais mode de défaillance fausserait une cotation, donc ce
 * calcul ne décide de rien : il ordonne des candidats qu'un humain confirme.
 *
 * <p>Fruste et déterministe, à dessein. Il tourne dans le chemin de création d'une
 * non-conformité : y brancher un service d'inférence ferait dépendre la saisie d'un
 * défaut au poste de la disponibilité de ce service. Le jour où l'on voudra des
 * embeddings, on fournira une autre implémentation.
 */
class FailureModeMatcherTest {

    static final UUID BAVURE = UUID.randomUUID();
    static final UUID ETIQUETTE = UUID.randomUUID();
    static final UUID SERRAGE = UUID.randomUUID();

    private final List<FailureModeMatcher.Candidate> candidates = List.of(
            new FailureModeMatcher.Candidate(BAVURE, "Bavure sur alésage", "Montage impossible"),
            new FailureModeMatcher.Candidate(ETIQUETTE, "Étiquette absente", "Traçabilité perdue"),
            new FailureModeMatcher.Candidate(SERRAGE, "Couple de serrage insuffisant", "Desserrage en service"));

    @Test
    void theClosestFailureModeComesFirst() {
        List<FailureModeMatcher.Suggestion> found = FailureModeMatcher.suggest(
                "Bavure constatée sur l'alésage de la pièce", candidates, 3);

        assertThat(found).isNotEmpty();
        assertThat(found.get(0).fmeaItemId()).isEqualTo(BAVURE);
    }

    @Test
    void accentsAndCaseDoNotChangeTheResult() {
        List<FailureModeMatcher.Suggestion> found = FailureModeMatcher.suggest(
                "ETIQUETTE ABSENTE constatee", candidates, 3);

        assertThat(found.get(0).fmeaItemId()).isEqualTo(ETIQUETTE);
    }

    @Test
    void anUnrelatedTextSuggestsNothing() {
        List<FailureModeMatcher.Suggestion> found = FailureModeMatcher.suggest(
                "Retard de livraison du transporteur", candidates, 3);

        assertThat(found).isEmpty();
    }

    @Test
    void stopWordsAloneNeverMatch() {
        // « sur la de le » ne partage que des mots vides avec les trois candidats :
        // sans filtrage, ils obtiendraient un score identique et le premier arrivé
        // serait proposé en tête, au hasard.
        List<FailureModeMatcher.Suggestion> found = FailureModeMatcher.suggest(
                "sur la de le et un une des", candidates, 3);

        assertThat(found).isEmpty();
    }

    @Test
    void atMostTheRequestedNumberOfSuggestionsComeBack() {
        List<FailureModeMatcher.Suggestion> found = FailureModeMatcher.suggest(
                "bavure etiquette serrage alesage tracabilite desserrage", candidates, 2);

        assertThat(found).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    void suggestionsAreOrderedByDecreasingScore() {
        List<FailureModeMatcher.Suggestion> found = FailureModeMatcher.suggest(
                "bavure alesage etiquette tracabilite", candidates, 3);

        assertThat(found).isSortedAccordingTo((a, b) -> Double.compare(b.score(), a.score()));
    }

    @Test
    void anEmptyCandidateListYieldsNothingRatherThanFailing() {
        assertThat(FailureModeMatcher.suggest("bavure", List.of(), 3)).isEmpty();
    }

    @Test
    void aNullTextYieldsNothingRatherThanFailing() {
        assertThat(FailureModeMatcher.suggest(null, candidates, 3)).isEmpty();
    }

    @Test
    void aZeroLimitYieldsNothing() {
        assertThat(FailureModeMatcher.suggest("bavure alesage", candidates, 0)).isEmpty();
    }

    @Test
    void aCandidateWithoutAnyUsableTermIsSkippedRatherThanFailing() {
        List<FailureModeMatcher.Candidate> empty = List.of(
                new FailureModeMatcher.Candidate(BAVURE, "", ""));

        assertThat(FailureModeMatcher.suggest("bavure alesage", empty, 3)).isEmpty();
    }

    @Test
    void theMatchedTermsAreReturnedSoTheSuggestionCanBeContested() {
        List<FailureModeMatcher.Suggestion> found = FailureModeMatcher.suggest(
                "Bavure sur l'alésage", candidates, 3);

        assertThat(found.get(0).matchedTerms()).contains("bavure").contains("alesage");
    }

    @Test
    void aSingleSharedTermDrownedInAVerboseTextIsNotProposed() {
        // Un seul terme partagé dans un texte long donne un Jaccard famélique.
        // Le seuil est là pour ça : une suggestion fausse coûte plus cher qu'une
        // absence de suggestion.
        List<FailureModeMatcher.Suggestion> found = FailureModeMatcher.suggest(
                "bavure incident survenu pendant equipe nuit vendredi janvier atelier usinage "
                        + "remonte responsable production apres controle final expedition",
                candidates, 3);

        assertThat(found).isEmpty();
    }
}
