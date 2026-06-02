package com.openlab.qualitos.quality.spc;

import com.openlab.qualitos.quality.aigateway.AiGatewayClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpcServiceTest {

    @Mock AiGatewayClient ai;
    @InjectMocks SpcService service;

    @Test
    void analyze_mapsFullGatewayResponse() {
        when(ai.detectSpc(eq(List.of(1.0, 2.0, 3.0)), isNull(), isNull())).thenReturn(Map.of(
                "n", 3,
                "out_of_control", true,
                "limits", Map.of(
                        "center_line", 2.0, "sigma", 0.5,
                        "ucl", 3.5, "lcl", 0.5, "estimated", true),
                "violations", List.of(Map.of(
                        "rule", "RULE_1",
                        "title", "Point hors limites 3σ",
                        "description", "1 point au-delà de l'UCL",
                        "point_indices", List.of(2),
                        "severity", "CRITICAL"))));

        SpcDto.AnalyzeResponse r = service.analyze(new SpcDto.AnalyzeRequest(List.of(1.0, 2.0, 3.0), null, null));

        assertThat(r.n()).isEqualTo(3);
        assertThat(r.outOfControl()).isTrue();
        assertThat(r.limits().centerLine()).isEqualTo(2.0);
        assertThat(r.limits().ucl()).isEqualTo(3.5);
        assertThat(r.limits().estimated()).isTrue();
        assertThat(r.violations()).singleElement().satisfies(v -> {
            assertThat(v.rule()).isEqualTo("RULE_1");
            assertThat(v.pointIndices()).containsExactly(2);
            assertThat(v.severity()).isEqualTo("CRITICAL");
        });
    }

    @Test
    void analyze_toleratesMissingOrMistypedFields() {
        // Réponse dégradée : champs absents/mal typés → valeurs sûres par défaut.
        when(ai.detectSpc(eq(List.of(5.0)), isNull(), isNull())).thenReturn(Map.of(
                "limits", "not-a-map",       // mauvais type → limites à 0
                "violations", "not-a-list")); // mauvais type → []

        SpcDto.AnalyzeResponse r = service.analyze(new SpcDto.AnalyzeRequest(List.of(5.0), null, null));

        assertThat(r.n()).isZero();
        assertThat(r.outOfControl()).isFalse();
        assertThat(r.limits().centerLine()).isZero();
        assertThat(r.limits().estimated()).isFalse();
        assertThat(r.violations()).isEmpty();
    }

    @Test
    void analyze_passesBaselineThrough() {
        when(ai.detectSpc(eq(List.of(1.0, 2.0)), eq(10.0), eq(2.0))).thenReturn(Map.of());

        service.analyze(new SpcDto.AnalyzeRequest(List.of(1.0, 2.0), 10.0, 2.0));

        ArgumentCaptor<Double> center = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> sigma = ArgumentCaptor.forClass(Double.class);
        verify(ai).detectSpc(eq(List.of(1.0, 2.0)), center.capture(), sigma.capture());
        assertThat(center.getValue()).isEqualTo(10.0);
        assertThat(sigma.getValue()).isEqualTo(2.0);
    }
}
