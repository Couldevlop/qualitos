package com.openlab.qualitos.quality.capa.effectiveness.application;

import com.openlab.qualitos.quality.capa.effectiveness.application.EvaluationCasePort.EvaluationCase;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessReview;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EvaluationStage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Le plan de travail des évaluations d'efficacité (ADR 0108).
 *
 * <p>Ce banc protège trois choses qui décident de l'écran sans se voir.
 *
 * <p><b>Le nombre de requêtes.</b> Trois pour tout le tableau, quel qu'en soit
 * le nombre de lignes. Au brouillon, le service demandait l'historique dossier
 * par dossier — une requête par ligne, exactement le défaut que le contrat du
 * port interdit. Le test le vérifie en comptant les appels, parce qu'un tableau
 * de vingt lignes ne se plaint pas et qu'un tableau de deux mille, si.
 *
 * <p><b>Les indicateurs comptés sur l'ENSEMBLE</b>, pas sur la page affichée :
 * un taux calculé sur les deux cents premières lignes serait faux sans le dire.
 *
 * <p><b>Le tri par urgence</b>, qui remplace un filtre de statut : ce qui est à
 * juger arrive en tête, et personne n'a à savoir quel statut demander.
 */
class EffectivenessBoardServiceTest {

    static final UUID TENANT = UUID.randomUUID();
    static final Instant T0 = Instant.parse("2026-11-01T08:00:00Z");

    EvaluationCasePort cases;
    EffectivenessReviewPort reviews;
    TenantProvider tenants;
    EffectivenessBoardService service;

    @BeforeEach
    void setUp() {
        cases = mock(EvaluationCasePort.class);
        reviews = mock(EffectivenessReviewPort.class);
        tenants = mock(TenantProvider.class);
        when(tenants.requireTenantId()).thenReturn(TENANT);
        service = new EffectivenessBoardService(cases, reviews, tenants);
    }

    // --- une ligne, et ce qu'elle dit ---

    @Test
    void aResolvedCaseWithAllItsActionsDoneIsWaitingToBeJudged() {
        donne(List.of(awaiting("CAPA-1", "Dérive dimensionnelle")), Map.of(), Map.of());

        EffectivenessBoardDto.Summary s = service.board();

        assertThat(s.rows()).hasSize(1);
        EffectivenessBoardDto.Row r = s.rows().get(0);
        assertThat(r.reference()).isEqualTo("CAPA-1");
        assertThat(r.title()).isEqualTo("Dérive dimensionnelle");
        assertThat(r.stage()).isEqualTo(EvaluationStage.AWAITING_EVALUATION);
        assertThat(r.evaluable()).isTrue();
        assertThat(r.lastReview()).isNull();
        assertThat(r.reviewCount()).isZero();
        assertThat(s.awaitingEvaluation()).isEqualTo(1);
        assertThat(s.total()).isEqualTo(1);
        assertThat(s.truncated()).isFalse();
    }

    @Test
    void theLastReviewAndTheirCountTravelWithTheRow() {
        EvaluationCase c = awaiting("CAPA-1", "Rebuts");
        EffectivenessReview last = review(c.id(), EffectivenessVerdict.PARTIAL, "3,0 ‰");
        donne(List.of(c), Map.of(c.id(), last), Map.of(c.id(), 3));

        EffectivenessBoardDto.Row r = service.board().rows().get(0);

        assertThat(r.lastReview()).isNotNull();
        assertThat(r.lastReview().verdict()).isEqualTo(EffectivenessVerdict.PARTIAL);
        assertThat(r.lastReview().observed()).isEqualTo("3,0 ‰");
        assertThat(r.lastReview().criterion()).isEqualTo("< 2 ‰ sur 3 mois");
        assertThat(r.reviewCount()).isEqualTo(3);
    }

    @Test
    void aCaseOnWhichClosureIsBlockedSaysSoAndSaysWhy() {
        // L'écran doit annoncer l'obstacle AVANT le clic. Deux motifs distincts,
        // tous deux repris tels quels : « bloqué » seul obligerait l'utilisateur
        // à cliquer pour apprendre ce qui bloque.
        EvaluationCase endiguement = new EvaluationCase(
                UUID.randomUUID(), "CAPA-C", "Endiguement seul", "MAJOR", "A. Diallo",
                "< 2 ‰", false, true, 1, 1, true, 0, null, T0);
        EvaluationCase ecartOuvert = new EvaluationCase(
                UUID.randomUUID(), "CAPA-N", "Écart encore ouvert", "MAJOR", "A. Diallo",
                "< 2 ‰", false, true, 1, 1, false, 2, null, T0);
        donne(List.of(endiguement, ecartOuvert), Map.of(), Map.of());

        Map<String, EffectivenessBoardDto.Row> parRef = parReference(service.board());

        assertThat(parRef.get("CAPA-C").closureBlocked()).isTrue();
        assertThat(parRef.get("CAPA-C").containmentOnly()).isTrue();
        assertThat(parRef.get("CAPA-C").openNcCount()).isZero();
        assertThat(parRef.get("CAPA-N").closureBlocked()).isTrue();
        assertThat(parRef.get("CAPA-N").containmentOnly()).isFalse();
        assertThat(parRef.get("CAPA-N").openNcCount()).isEqualTo(2);
    }

    @Test
    void anUnblockedCaseSaysThatToo() {
        donne(List.of(awaiting("CAPA-1", "Rebuts")), Map.of(), Map.of());
        assertThat(service.board().rows().get(0).closureBlocked()).isFalse();
    }

    // --- les indicateurs ---

    @Test
    void theEffectiveRateIsComputedOnEvaluatedCasesOnly() {
        // Le dénominateur est le nombre de dossiers JUGÉS, pas le nombre de
        // dossiers. Compter les non-jugés comme des échecs ferait baisser le
        // taux à chaque ouverture de dossier — un indicateur qui punit le fait
        // de travailler.
        EvaluationCase a = closed("CAPA-A");
        EvaluationCase b = closed("CAPA-B");
        EvaluationCase c = awaiting("CAPA-C", "Jamais jugé");
        donne(List.of(a, b, c),
                Map.of(a.id(), review(a.id(), EffectivenessVerdict.EFFECTIVE, "1 ‰"),
                        b.id(), review(b.id(), EffectivenessVerdict.INEFFECTIVE, "8 ‰")),
                Map.of());

        EffectivenessBoardDto.Summary s = service.board();

        assertThat(s.evaluated()).isEqualTo(2);
        assertThat(s.effectiveRatePercent()).isEqualTo(50);
        assertThat(s.total()).isEqualTo(3);
    }

    @Test
    void aPartialVerdictDoesNotCountAsEffective() {
        EvaluationCase a = closed("CAPA-A");
        donne(List.of(a), Map.of(a.id(), review(a.id(), EffectivenessVerdict.PARTIAL, "3 ‰")),
                Map.of());

        assertThat(service.board().effectiveRatePercent()).isZero();
    }

    @Test
    void withNothingEvaluatedTheRateIsAbsentRatherThanZero() {
        // Zéro pour cent se lit « tout a échoué ». Vide se lit « rien n'a encore
        // été jugé », qui est la vérité d'un tenant qui démarre.
        donne(List.of(awaiting("CAPA-1", "Rebuts")), Map.of(), Map.of());

        assertThat(service.board().effectiveRatePercent()).isNull();
    }

    @Test
    void anEmptyBoardIsStillAValidAnswer() {
        donne(List.of(), Map.of(), Map.of());

        EffectivenessBoardDto.Summary s = service.board();

        assertThat(s.rows()).isEmpty();
        assertThat(s.total()).isZero();
        assertThat(s.effectiveRatePercent()).isNull();
        assertThat(s.truncated()).isFalse();
    }

    @Test
    void theCountersSeparateWhatIsStillOpenFromWhatIsClosed() {
        EvaluationCase clos = closed("CAPA-CLOS");
        EvaluationCase aJuger = awaiting("CAPA-JUGE", "À juger");
        EvaluationCase partiel = reopenedWith("CAPA-PART", EffectivenessVerdict.PARTIAL);
        EvaluationCase inefficace = reopenedWith("CAPA-INEF", EffectivenessVerdict.INEFFECTIVE);
        Map<UUID, EffectivenessReview> last = new HashMap<>();
        last.put(clos.id(), review(clos.id(), EffectivenessVerdict.EFFECTIVE, "1 ‰"));
        last.put(partiel.id(), review(partiel.id(), EffectivenessVerdict.PARTIAL, "3 ‰"));
        last.put(inefficace.id(), review(inefficace.id(), EffectivenessVerdict.INEFFECTIVE, "9 ‰"));
        donne(List.of(clos, aJuger, partiel, inefficace), last, Map.of());

        EffectivenessBoardDto.Summary s = service.board();

        assertThat(s.stillOpen()).isEqualTo(3);
        assertThat(s.awaitingEvaluation()).isEqualTo(1);
        assertThat(s.partial()).isEqualTo(1);
        assertThat(s.reopened()).isEqualTo(1);
        assertThat(s.evaluated()).isEqualTo(3);
    }

    @Test
    void everyOpenCaseWithoutACriterionIsCounted() {
        // Pas seulement ceux déjà prêts à juger : à ce moment-là, il est trop
        // tard pour poser un critère « à l'avance ». Un dossier clos, en
        // revanche, ne compte plus — on ne peut plus rien y poser.
        EvaluationCase sansCritereEnCours = new EvaluationCase(
                UUID.randomUUID(), "CAPA-1", "Sans critère, en cours", "MINOR", null,
                null, false, false, 2, 1, false, 0, null, T0);
        EvaluationCase sansCritereAJuger = new EvaluationCase(
                UUID.randomUUID(), "CAPA-2", "Sans critère, à juger", "MINOR", null,
                "   ", false, true, 1, 1, false, 0, null, T0);
        EvaluationCase avecCritere = awaiting("CAPA-3", "Avec critère");
        EvaluationCase closSansCritere = new EvaluationCase(
                UUID.randomUUID(), "CAPA-4", "Clos sans critère", "MINOR", null,
                null, true, false, 1, 1, false, 0, null, T0);
        donne(List.of(sansCritereEnCours, sansCritereAJuger, avecCritere, closSansCritere),
                Map.of(), Map.of());

        assertThat(service.board().missingCriterion()).isEqualTo(2);
    }

    // --- tri et borne ---

    @Test
    void whatIsToBeJudgedComesFirst() {
        EvaluationCase enCours = new EvaluationCase(
                UUID.randomUUID(), "CAPA-ENCOURS", "En cours", "MINOR", null,
                "< 2 ‰", false, false, 2, 1, false, 0, null, T0);
        EvaluationCase clos = closed("CAPA-CLOS");
        EvaluationCase aJuger = awaiting("CAPA-JUGE", "À juger");
        EvaluationCase partiel = reopenedWith("CAPA-PART", EffectivenessVerdict.PARTIAL);
        EvaluationCase reouvert = reopenedWith("CAPA-REOUV", EffectivenessVerdict.INEFFECTIVE);
        Map<UUID, EffectivenessReview> last = new HashMap<>();
        last.put(clos.id(), review(clos.id(), EffectivenessVerdict.EFFECTIVE, "1 ‰"));
        last.put(partiel.id(), review(partiel.id(), EffectivenessVerdict.PARTIAL, "3 ‰"));
        last.put(reouvert.id(), review(reouvert.id(), EffectivenessVerdict.INEFFECTIVE, "9 ‰"));
        donne(List.of(enCours, clos, aJuger, partiel, reouvert), last, Map.of());

        assertThat(service.board().rows()).extracting(EffectivenessBoardDto.Row::reference)
                .containsExactly("CAPA-JUGE", "CAPA-PART", "CAPA-REOUV", "CAPA-ENCOURS",
                        "CAPA-CLOS");
    }

    @Test
    void atEqualUrgencyTheMostRecentlyTouchedComesFirst() {
        EvaluationCase vieux = withUpdatedAt("CAPA-VIEUX", T0);
        EvaluationCase recent = withUpdatedAt("CAPA-RECENT", T0.plusSeconds(86_400));
        donne(List.of(vieux, recent), Map.of(), Map.of());

        assertThat(service.board().rows()).extracting(EffectivenessBoardDto.Row::reference)
                .containsExactly("CAPA-RECENT", "CAPA-VIEUX");
    }

    @Test
    void aCaseWithoutADateGoesLastRatherThanBreakingTheSort() {
        EvaluationCase sansDate = withUpdatedAt("CAPA-SANS", null);
        EvaluationCase avecDate = withUpdatedAt("CAPA-AVEC", T0);
        donne(List.of(sansDate, avecDate), Map.of(), Map.of());

        assertThat(service.board().rows()).extracting(EffectivenessBoardDto.Row::reference)
                .containsExactly("CAPA-AVEC", "CAPA-SANS");
    }

    @Test
    void theBoardIsBoundedAndSaysSoWhileCountingEverything() {
        // La borne porte sur ce qu'on AFFICHE. Les indicateurs, eux, portent sur
        // tout : un taux calculé sur les deux cents premières lignes serait faux
        // sans le dire, et c'est précisément le chiffre qu'une direction lit.
        int n = EffectivenessBoardService.MAX_ROWS + 50;
        List<EvaluationCase> beaucoup = new ArrayList<>(n);
        Map<UUID, EffectivenessReview> last = new HashMap<>();
        for (int i = 0; i < n; i++) {
            EvaluationCase c = closed("CAPA-" + i);
            beaucoup.add(c);
            last.put(c.id(), review(c.id(), EffectivenessVerdict.EFFECTIVE, "1 ‰"));
        }
        donne(beaucoup, last, Map.of());

        EffectivenessBoardDto.Summary s = service.board();

        assertThat(s.rows()).hasSize(EffectivenessBoardService.MAX_ROWS);
        assertThat(s.truncated()).isTrue();
        assertThat(s.total()).isEqualTo(n);
        assertThat(s.evaluated()).isEqualTo(n);
        assertThat(s.effectiveRatePercent()).isEqualTo(100);
    }

    @Test
    void exactlyTheBoundIsNotTruncated() {
        List<EvaluationCase> pile = new ArrayList<>();
        for (int i = 0; i < EffectivenessBoardService.MAX_ROWS; i++) {
            pile.add(closed("CAPA-" + i));
        }
        donne(pile, Map.of(), Map.of());

        EffectivenessBoardDto.Summary s = service.board();

        assertThat(s.truncated()).isFalse();
        assertThat(s.rows()).hasSize(EffectivenessBoardService.MAX_ROWS);
    }

    // --- le nombre de requêtes ---

    @Test
    void theWholeBoardCostsThreeQueriesWhateverItsSize() {
        List<EvaluationCase> vingt = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            vingt.add(awaiting("CAPA-" + i, "Dossier " + i));
        }
        donne(vingt, Map.of(), Map.of());

        service.board();

        verify(cases).findInScope(TENANT);
        verify(reviews).findLastPerCase(TENANT);
        verify(reviews).countPerCase(TENANT);
        // Le défaut que ce test attrape : une lecture d'historique PAR LIGNE.
        verifyNoMoreInteractions(reviews);
    }

    @Test
    void theTenantAlwaysComesFromTheContextNeverFromTheCaller() {
        donne(List.of(), Map.of(), Map.of());

        service.board();

        verify(tenants).requireTenantId();
        verify(cases).findInScope(TENANT);
    }

    // --- l'historique d'un dossier ---

    @Test
    void theHistoryOfACaseIsReturnedInTheOrderThePortGivesIt() {
        UUID capa = UUID.randomUUID();
        EffectivenessReview recent = review(capa, EffectivenessVerdict.EFFECTIVE, "1 ‰");
        EffectivenessReview ancien = review(capa, EffectivenessVerdict.INEFFECTIVE, "9 ‰");
        when(reviews.findByCase(TENANT, capa)).thenReturn(List.of(recent, ancien));

        List<EffectivenessBoardDto.Review> h = service.history(capa);

        assertThat(h).extracting(EffectivenessBoardDto.Review::verdict)
                .containsExactly(EffectivenessVerdict.EFFECTIVE, EffectivenessVerdict.INEFFECTIVE);
        assertThat(h.get(0).observed()).isEqualTo("1 ‰");
        assertThat(h.get(1).observed()).isEqualTo("9 ‰");
    }

    @Test
    void aCaseNeverEvaluatedHasAnEmptyHistory() {
        UUID capa = UUID.randomUUID();
        when(reviews.findByCase(TENANT, capa)).thenReturn(List.of());

        assertThat(service.history(capa)).isEmpty();
    }

    // --- outillage ---

    private void donne(List<EvaluationCase> inScope, Map<UUID, EffectivenessReview> last,
                       Map<UUID, Integer> counts) {
        when(cases.findInScope(TENANT)).thenReturn(inScope);
        when(reviews.findLastPerCase(TENANT)).thenReturn(last);
        when(reviews.countPerCase(TENANT)).thenReturn(counts);
    }

    private static Map<String, EffectivenessBoardDto.Row> parReference(
            EffectivenessBoardDto.Summary s) {
        Map<String, EffectivenessBoardDto.Row> parRef = new HashMap<>();
        s.rows().forEach(r -> parRef.put(r.reference(), r));
        return parRef;
    }

    private static EvaluationCase awaiting(String reference, String title) {
        return new EvaluationCase(UUID.randomUUID(), reference, title, "MAJOR", "A. Diallo",
                "< 2 ‰", false, true, 1, 1, false, 0, null, T0);
    }

    private static EvaluationCase closed(String reference) {
        return new EvaluationCase(UUID.randomUUID(), reference, "Clos", "MAJOR", "A. Diallo",
                "< 2 ‰", true, false, 1, 1, false, 0, null, T0);
    }

    private static EvaluationCase reopenedWith(String reference, EffectivenessVerdict ignored) {
        return new EvaluationCase(UUID.randomUUID(), reference, "Reparti au travail", "MAJOR",
                "A. Diallo", "< 2 ‰", false, false, 2, 1, false, 0, null, T0);
    }

    private static EvaluationCase withUpdatedAt(String reference, Instant updatedAt) {
        return new EvaluationCase(UUID.randomUUID(), reference, "Dossier", "MINOR", null,
                "< 2 ‰", false, true, 1, 1, false, 0, null, updatedAt);
    }

    private static EffectivenessReview review(UUID capaId, EffectivenessVerdict verdict,
                                              String observed) {
        return EffectivenessReview.record(UUID.randomUUID(), TENANT, capaId, verdict,
                "< 2 ‰ sur 3 mois", "7,4 ‰", "< 2 ‰", observed, null, null, "A. Diallo", T0);
    }
}
