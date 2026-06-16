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
}
