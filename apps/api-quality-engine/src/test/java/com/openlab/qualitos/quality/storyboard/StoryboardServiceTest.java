package com.openlab.qualitos.quality.storyboard;

import com.openlab.qualitos.quality.aigateway.AiCompletionResult;
import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import com.openlab.qualitos.quality.aigateway.AiGatewayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoryboardServiceTest {

    @Mock AiGatewayClient ai;
    @InjectMocks StoryboardService service;

    private StoryboardDto.StoryboardRequest sampleRequest() {
        return new StoryboardDto.StoryboardRequest(
                "Mai 2026",
                "Site de Lyon",
                List.of(
                        new StoryboardDto.IndicatorPoint("Taux de NC", "1,8", "-12 %", "< 2", "%"),
                        new StoryboardDto.IndicatorPoint("Délai CAPA", "26", "stable", "< 30", "j")));
    }

    @Test
    void generate_buildsDeterministicPrompt_andCommentsOnlyProvidedFigures() {
        ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        when(ai.complete(system.capture(), user.capture(), anyInt()))
                .thenReturn(new AiCompletionResult("Le récit.", "ollama", 42, 100));

        StoryboardDto.StoryboardResponse r = service.generate(sampleRequest());

        // Consigne système : anti-hallucination explicite.
        assertThat(system.getValue())
                .contains("UNIQUEMENT les chiffres fournis")
                .contains("n'invente AUCUN chiffre");
        // Bloc données : période, contexte, chaque indicateur avec unité/tendance/cible.
        String u = user.getValue();
        assertThat(u).contains("Période : Mai 2026.");
        assertThat(u).contains("Contexte : Site de Lyon.");
        assertThat(u).contains("Taux de NC : 1,8 % ; tendance -12 % ; cible < 2");
        assertThat(u).contains("Délai CAPA : 26 j ; tendance stable ; cible < 30");
        assertThat(u).endsWith("Rédige le récit.");
        // Mapping réponse + rappel des chiffres source (explicabilité §12.3).
        assertThat(r.narrative()).isEqualTo("Le récit.");
        assertThat(r.provider()).isEqualTo("ollama");
        assertThat(r.period()).isEqualTo("Mai 2026");
        assertThat(r.sources()).hasSize(2);
        assertThat(r.sources().get(0).label()).isEqualTo("Taux de NC");
    }

    @Test
    void generate_omitsOptionalFields_whenAbsentOrBlank() {
        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        when(ai.complete(anyString(), user.capture(), anyInt()))
                .thenReturn(new AiCompletionResult("ok", "fallback", 1, 1));

        StoryboardDto.StoryboardRequest req = new StoryboardDto.StoryboardRequest(
                "T1 2026", "  ",
                List.of(new StoryboardDto.IndicatorPoint("FPY", "97", "", null, null)));

        service.generate(req);

        String u = user.getValue();
        // Pas de contexte (blanc), pas d'unité/tendance/cible (null/blanc) : ligne minimale.
        assertThat(u).doesNotContain("Contexte :");
        assertThat(u).contains("FPY : 97\n");
        assertThat(u).doesNotContain("tendance");
        assertThat(u).doesNotContain("cible");
    }

    @Test
    void generate_propagatesGatewayFailure() {
        when(ai.complete(anyString(), anyString(), anyInt()))
                .thenThrow(new AiGatewayException("ai-service down"));

        assertThatThrownBy(() -> service.generate(sampleRequest()))
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    void generate_stripsNarrative_andToleratesNullText() {
        when(ai.complete(anyString(), anyString(), anyInt()))
                .thenReturn(new AiCompletionResult(null, "ollama", 0, 0));

        StoryboardDto.StoryboardResponse r = service.generate(sampleRequest());

        assertThat(r.narrative()).isEmpty();
        verify(ai).complete(anyString(), anyString(), anyInt());
    }

    @Test
    void targetMet_isComputed_notLeftToTheModel() {
        assertThat(StoryboardService.targetMet("26", "< 30")).isTrue();
        assertThat(StoryboardService.targetMet("1,8", "< 2")).isTrue();
        assertThat(StoryboardService.targetMet("97,4", "> 98")).isFalse();
        assertThat(StoryboardService.targetMet("8/10", "10/10")).isFalse();
        assertThat(StoryboardService.targetMet("30", "<= 30")).isTrue();
        assertThat(StoryboardService.targetMet("98", ">= 98")).isTrue();
        assertThat(StoryboardService.targetMet("élevé", "< 30")).isNull();
        assertThat(StoryboardService.targetMet("26", "réduire")).isNull();
        assertThat(StoryboardService.targetMet(null, "< 2")).isNull();
    }

    @Test
    void prompt_carriesTheComputedStatus() {
        String user = service.debugPrompts(sampleRequest()).get(1);
        assertThat(user).contains("Délai CAPA : 26 j ; tendance stable ; cible < 30 ; statut : cible ATTEINTE");
    }

    @Test
    void unverifiedNumbers_flagsWhatTheSourcesDoNotContain() {
        assertThat(StoryboardService.unverifiedNumbers(
                "En mai 2026, le taux de NC passe de 3 % à 1,8 % (-12 %) ; délai CAPA 26 j, 24 j avant.",
                sampleRequest()))
                .containsExactly("3", "24");
        assertThat(StoryboardService.unverifiedNumbers("Taux de NC 1.8 %, cible 2.", sampleRequest())).isEmpty();
    }

    @Test
    void inventedNumbers_triggerOneCorrection() {
        when(ai.complete(anyString(), anyString(), anyInt()))
                .thenReturn(new AiCompletionResult("Le taux de NC passe de 3 % à 1,8 %.", "ollama", 1, 1))
                .thenReturn(new AiCompletionResult("Le taux de NC recule à 1,8 %.", "ollama", 1, 1));
        StoryboardDto.StoryboardResponse r = service.generate(sampleRequest());
        assertThat(r.mode()).isEqualTo("ai-corrected");
        assertThat(r.narrative()).isEqualTo("Le taux de NC recule à 1,8 %.");
        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        verify(ai, org.mockito.Mockito.times(2)).complete(anyString(), user.capture(), anyInt());
        assertThat(user.getAllValues().get(1)).contains("chiffres absents des données : 3");
    }

    @Test
    void inventedTwice_fallsBackToTheFactualNarrative() {
        when(ai.complete(anyString(), anyString(), anyInt()))
                .thenReturn(new AiCompletionResult("De 3 % à 1,8 %.", "ollama", 1, 1));
        StoryboardDto.StoryboardResponse r = service.generate(sampleRequest());
        assertThat(r.mode()).isEqualTo("factual");
        assertThat(r.narrative()).isEqualTo(StoryboardService.factualNarrative(sampleRequest()));
        assertThat(r.unverifiedNumbers()).isEmpty();
    }

    private StoryboardDto.StoryboardRequest exampleRequest() {
        return new StoryboardDto.StoryboardRequest("Mai 2026", "Site de Lyon, atelier mécanique", List.of(
                new StoryboardDto.IndicatorPoint("Taux de NC", "1,8", "-12 %", "< 2", "%"),
                new StoryboardDto.IndicatorPoint("Délai moyen de clôture CAPA", "26", "stable", "< 30", "j"),
                new StoryboardDto.IndicatorPoint("First Pass Yield", "97,4", "+1,2 pt", "> 98", "%"),
                new StoryboardDto.IndicatorPoint("Audits réalisés vs planifiés", "8/10", "2 retards", "10/10", null)));
    }

    /** Le récit rendu en préproduction : tous ses nombres viennent des données, et il est faux. */
    @Test
    void errors_catchAStoryMadeOfTrueNumbers() {
        List<String> errors = StoryboardService.errors(
                "Le taux de non-conformité ( NC) est passé de 1,8 % à -12 %, un écart positif par rapport "
                        + "à la cible fixée (-2 %). Le délai moyen de clôture des CAPA a connu une légère "
                        + "progression, passant de 26 jours à 26 j. Le First Pass Yield a connu une légère "
                        + "baisse à 97,4 %.", exampleRequest());
        assertThat(errors).anySatisfy(e -> assertThat(e).contains("chiffres absents").contains("-2"));
        assertThat(errors).anySatisfy(e -> assertThat(e).contains("« de 1,8 % à -12 % »"));
        assertThat(errors).anySatisfy(e -> assertThat(e).contains("« de 26 jours à 26 j »"));
        assertThat(errors).anySatisfy(e -> assertThat(e).contains("First Pass Yield").contains("+1,2 pt"));
        assertThat(errors).hasSize(4);
    }

    @Test
    void errors_acceptAFaithfulStory() {
        assertThat(StoryboardService.errors(
                "En mai 2026, le taux de NC recule de 12 % et s'établit à 1,8 %, sous la cible de 2 %. "
                        + "Le délai moyen de clôture CAPA reste à 26 j. Le First Pass Yield est en hausse "
                        + "de 1,2 pt à 97,4 %, encore sous la cible de 98 %. Les audits, 8/10, comptent 2 retards.",
                exampleRequest())).isEmpty();
        assertThat(StoryboardService.errors(
                StoryboardService.factualNarrative(exampleRequest()), exampleRequest())).isEmpty();
    }

    @Test
    void errors_flagAMoveOnAStableIndicator_andTheWrongSign() {
        assertThat(StoryboardService.errors("Le délai moyen de clôture CAPA augmente, à 26 j.", exampleRequest()))
                .singleElement().asString().contains("stable");
        assertThat(StoryboardService.unverifiedNumbers("Tendance +12 % du taux de NC.", exampleRequest()))
                .containsExactly("+12");
        assertThat(StoryboardService.unverifiedNumbers("Entre 14-12 % du taux de NC.", exampleRequest()))
                .containsExactly("14");
    }

    @Test
    void direction_readsTheTrend() {
        assertThat(StoryboardService.direction("-12 %")).isEqualTo(-1);
        assertThat(StoryboardService.direction("+1,2 pt")).isEqualTo(1);
        assertThat(StoryboardService.direction("−3")).isEqualTo(-1);
        assertThat(StoryboardService.direction("stable")).isZero();
        assertThat(StoryboardService.direction("en hausse")).isEqualTo(1);
        assertThat(StoryboardService.direction("en baisse")).isEqualTo(-1);
        assertThat(StoryboardService.direction("2 retards")).isNull();
        assertThat(StoryboardService.direction(" ")).isNull();
        assertThat(StoryboardService.direction(null)).isNull();
    }

    @Test
    void aWrongDirection_triggersTheCorrection_thenTheFactualNarrative() {
        when(ai.complete(anyString(), anyString(), anyInt()))
                .thenReturn(new AiCompletionResult("Le First Pass Yield baisse à 97,4 %.", "ollama", 1, 1));
        StoryboardDto.StoryboardResponse r = service.generate(exampleRequest());
        assertThat(r.mode()).isEqualTo("factual");
        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        verify(ai, org.mockito.Mockito.times(2)).complete(anyString(), user.capture(), anyInt());
        assertThat(user.getAllValues().get(1)).contains("« First Pass Yield » a pour tendance +1,2 pt");
    }

    @Test
    void factualNarrative_saysOnlyWhatTheDataSays() {
        String text = StoryboardService.factualNarrative(sampleRequest());
        assertThat(text).isEqualTo("Sur la période Mai 2026 (Site de Lyon) : "
                + "Taux de NC s'établit à 1,8 %, tendance -12 % ; la cible < 2 est atteinte. "
                + "Délai CAPA s'établit à 26 j, tendance stable ; la cible < 30 est atteinte.");
        assertThat(StoryboardService.unverifiedNumbers(text, sampleRequest())).isEmpty();
    }
}
