package com.openlab.qualitos.quality.capa.effectiveness.web;

import com.openlab.qualitos.quality.capa.CapaNotFoundException;
import com.openlab.qualitos.quality.capa.effectiveness.application.CapaEffectivenessDto;
import com.openlab.qualitos.quality.capa.effectiveness.application.CapaEffectivenessService;
import com.openlab.qualitos.quality.capa.effectiveness.application.EffectivenessBoardDto;
import com.openlab.qualitos.quality.capa.effectiveness.application.EffectivenessBoardService;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EffectivenessVerdict;
import com.openlab.qualitos.quality.capa.effectiveness.domain.EvaluationStage;
import com.openlab.qualitos.quality.capa.effectiveness.domain.MeasurementStatus;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * L'API de l'efficacité CAPA.
 *
 * <p>Lecture ouverte à tout utilisateur authentifié du client, mais sur les
 * seuls dossiers que la portée du registre CAPA lui laisse voir (ADR 0081) —
 * la restriction est tenue par le service, et ce banc protège ce que la couche
 * web en fait : un dossier hors de portée répond 404, comme sa propre fiche.
 *
 * <p>Le reste tient à la borne de la fenêtre — un paramètre de requête finit
 * toujours par recevoir une valeur absurde — et au fait qu'un anonyme
 * n'obtienne rien.
 */
@Tag("web")
@WebMvcTest(controllers = CapaEffectivenessController.class)
@Import(MethodSecurityTestConfig.class)
class CapaEffectivenessControllerTest {

    static final String URL = "/api/v1/capa/effectiveness";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CapaEffectivenessService service;

    @MockitoBean
    EffectivenessBoardService board;

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void itRendersTheSummaryAndItsRows() throws Exception {
        when(service.measure(6)).thenReturn(summary());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.windowMonths").value(6))
                .andExpect(jsonPath("$.averageRatePercent").value(71))
                .andExpect(jsonPath("$.rows[0].title").value("Dérive dimensionnelle"))
                .andExpect(jsonPath("$.rows[0].ratePercent").value(92))
                .andExpect(jsonPath("$.rows[0].status").value("MEASURED"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void sixMonthsIsTheDefaultWindow() throws Exception {
        when(service.measure(6)).thenReturn(summary());

        mockMvc.perform(get(URL)).andExpect(status().isOk());

        verify(service).measure(6);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void anotherWindowIsHonoured() throws Exception {
        when(service.measure(12)).thenReturn(summary());

        mockMvc.perform(get(URL).param("months", "12")).andExpect(status().isOk());

        verify(service).measure(12);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void anAbsurdWindowIsRefusedWithoutReachingTheService() throws Exception {
        mockMvc.perform(get(URL).param("months", "0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL).param("months", "99"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    @WithAnonymousUser
    void anAnonymousCallerGetsNothing() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void anEmptySummaryIsStillAValidAnswer() throws Exception {
        when(service.measure(anyInt())).thenReturn(
                new CapaEffectivenessDto.Summary(6, 0, 0, 0, null, 0, 0, false, List.of()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows").isEmpty())
                .andExpect(jsonPath("$.averageRatePercent").doesNotExist());
    }

    // --- le plan de travail des évaluations (ADR 0108) ---

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void itRendersTheEvaluationBoard() throws Exception {
        when(board.board()).thenReturn(boardSummary());

        mockMvc.perform(get(URL + "/board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.effectiveRatePercent").value(50))
                .andExpect(jsonPath("$.awaitingEvaluation").value(1))
                .andExpect(jsonPath("$.missingCriterion").value(1))
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.truncated").value(false))
                .andExpect(jsonPath("$.rows[0].reference").value("CAPA-2026-0007"))
                .andExpect(jsonPath("$.rows[0].stage").value("AWAITING_EVALUATION"))
                .andExpect(jsonPath("$.rows[0].evaluable").value(true))
                .andExpect(jsonPath("$.rows[0].criterion").value("< 2 ‰ sur 3 mois"))
                .andExpect(jsonPath("$.rows[0].lastReview.verdict").value("PARTIAL"))
                .andExpect(jsonPath("$.rows[0].lastReview.observed").value("3,1 ‰"))
                .andExpect(jsonPath("$.rows[0].reviewCount").value(2));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void theBoardTakesNoParameterSoNoneCanBeWrong() throws Exception {
        // Le tri par urgence remplace un filtre de statut : ce qui est à juger
        // arrive en tête, et personne n'a à savoir quel statut demander. Un
        // paramètre inconnu est donc ignoré, pas honoré par accident.
        when(board.board()).thenReturn(boardSummary());

        mockMvc.perform(get(URL + "/board").param("stage", "N'IMPORTE_QUOI"))
                .andExpect(status().isOk());

        verify(board).board();
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void anEmptyBoardIsStillAValidAnswerToo() throws Exception {
        when(board.board()).thenReturn(new EffectivenessBoardDto.Summary(
                null, 0, 0, 0, 0, 0, 0, 0, false, List.of()));

        mockMvc.perform(get(URL + "/board"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows").isEmpty())
                .andExpect(jsonPath("$.effectiveRatePercent").doesNotExist());
    }

    @Test
    @WithAnonymousUser
    void anAnonymousCallerGetsNoBoard() throws Exception {
        mockMvc.perform(get(URL + "/board")).andExpect(status().isUnauthorized());

        verifyNoInteractions(board);
    }

    // --- l'historique d'un dossier ---

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void itRendersTheHistoryOfACaseNewestFirst() throws Exception {
        UUID capa = UUID.randomUUID();
        when(board.history(capa)).thenReturn(List.of(
                reviewOf(EffectivenessVerdict.EFFECTIVE, "1,1 ‰"),
                reviewOf(EffectivenessVerdict.INEFFECTIVE, "9,2 ‰")));

        mockMvc.perform(get(URL + "/cases/" + capa + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].verdict").value("EFFECTIVE"))
                .andExpect(jsonPath("$[0].observed").value("1,1 ‰"))
                .andExpect(jsonPath("$[0].reviewerName").value("A. Diallo"))
                .andExpect(jsonPath("$[1].verdict").value("INEFFECTIVE"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void aCaseNeverEvaluatedHasAnEmptyHistoryRatherThanAnError() throws Exception {
        UUID capa = UUID.randomUUID();
        when(board.history(capa)).thenReturn(List.of());

        mockMvc.perform(get(URL + "/cases/" + capa + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void anIdentifierThatIsNotOneIsRefusedWithoutReachingTheService() throws Exception {
        mockMvc.perform(get(URL + "/cases/pas-un-identifiant/reviews"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(board);
    }

    @Test
    @WithAnonymousUser
    void anAnonymousCallerGetsNoHistory() throws Exception {
        mockMvc.perform(get(URL + "/cases/" + UUID.randomUUID() + "/reviews"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(board);
    }

    private EffectivenessBoardDto.Summary boardSummary() {
        EffectivenessBoardDto.Row aJuger = new EffectivenessBoardDto.Row(
                UUID.randomUUID(), "CAPA-2026-0007", "Dérive dimensionnelle", "HIGH",
                "A. Diallo", EvaluationStage.AWAITING_EVALUATION, true, 2, 2,
                "< 2 ‰ sur 3 mois", reviewOf(EffectivenessVerdict.PARTIAL, "3,1 ‰"), 2,
                false, false, 0, Instant.parse("2026-11-05T09:00:00Z"));
        EffectivenessBoardDto.Row clos = new EffectivenessBoardDto.Row(
                UUID.randomUUID(), "CAPA-2026-0003", "Étiquetage", "LOW",
                null, EvaluationStage.CLOSED_EFFECTIVE, false, 1, 1,
                null, reviewOf(EffectivenessVerdict.EFFECTIVE, "0,8 ‰"), 1,
                false, false, 0, Instant.parse("2026-10-02T09:00:00Z"));
        return new EffectivenessBoardDto.Summary(50, 2, 1, 1, 0, 0, 1, 2, false,
                List.of(aJuger, clos));
    }

    private static EffectivenessBoardDto.Review reviewOf(EffectivenessVerdict verdict,
                                                          String observed) {
        return new EffectivenessBoardDto.Review(UUID.randomUUID(), verdict,
                "< 2 ‰ sur 3 mois", "7,4 ‰", "< 2 ‰", observed,
                "Mesuré sur les lots 2411 à 2418", "A. Diallo",
                Instant.parse("2026-11-06T10:15:00Z"));
    }

    private CapaEffectivenessDto.Summary summary() {
        CapaEffectivenessDto.Row row = new CapaEffectivenessDto.Row(
                UUID.randomUUID(), "Dérive dimensionnelle", "MAJOR",
                Instant.parse("2026-03-12T09:00:00Z"), MeasurementStatus.MEASURED,
                12, 1, 92, false, 180, 180, Boolean.TRUE, true);
        return new CapaEffectivenessDto.Summary(6, 1, 0, 0, 71, 0, 0, false, List.of(row));
    }

    @Test
    @WithMockUser(username = "lea", authorities = "ROLE_user")
    void history_caseOutOfScope_returns404() throws Exception {
        // Le service refuse par 404 un dossier que la personne n'a pas le droit
        // d'ouvrir (ADR 0081). Ce qui compte ici, c'est que la couche web rende
        // bien 404 et non 500 : un identifiant de dossier ne doit pas suffire à
        // apprendre qui a jugé, contre quel critère et avec quels relevés.
        UUID capa = UUID.randomUUID();
        when(board.history(capa)).thenThrow(new CapaNotFoundException(capa));

        mockMvc.perform(get("/api/v1/capa/effectiveness/cases/" + capa + "/reviews"))
                .andExpect(status().isNotFound());
    }
}
