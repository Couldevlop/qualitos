package com.openlab.qualitos.quality.complaintnlp;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComplaintNlpServiceTest {

    @Mock AiGatewayClient ai;
    @InjectMocks ComplaintNlpService service;

    @Test
    void analyze_mapsGatewayResponse() {
        when(ai.analyzeComplaints(any(), isNull())).thenReturn(Map.of(
                "n", 2,
                "critical_count", 1,
                "insights", List.of(
                        Map.of("index", 0, "sentiment", -0.8, "sentiment_label", "negative",
                                "category", "securite", "critical", true),
                        Map.of("index", 1, "sentiment", 0.7, "sentiment_label", "positive",
                                "category", "service", "critical", false))));

        ComplaintNlpDto.AnalyzeResponse r = service.analyze(new ComplaintNlpDto.AnalyzeRequest(
                List.of("dangereux", "service parfait"), null));

        assertThat(r.n()).isEqualTo(2);
        assertThat(r.criticalCount()).isEqualTo(1);
        assertThat(r.insights()).hasSize(2);
        assertThat(r.insights().get(0).critical()).isTrue();
        assertThat(r.insights().get(0).category()).isEqualTo("securite");
        assertThat(r.insights().get(1).sentimentLabel()).isEqualTo("positive");
    }

    @Test
    void analyze_toleratesMissingFields() {
        when(ai.analyzeComplaints(any(), any())).thenReturn(Map.of("insights", "nope"));
        ComplaintNlpDto.AnalyzeResponse r = service.analyze(
                new ComplaintNlpDto.AnalyzeRequest(List.of("x"), null));
        assertThat(r.n()).isZero();
        assertThat(r.insights()).isEmpty();
    }

    @Test
    void analyze_passesCustomCategories() {
        when(ai.analyzeComplaints(any(), any())).thenReturn(Map.of());
        Map<String, List<String>> cats = Map.of("hygiene", List.of("sale", "hygiene"));

        service.analyze(new ComplaintNlpDto.AnalyzeRequest(List.of("chambre sale"), cats));

        ArgumentCaptor<Map<String, List<String>>> cap = ArgumentCaptor.forClass(Map.class);
        verify(ai).analyzeComplaints(any(), cap.capture());
        assertThat(cap.getValue()).containsKey("hygiene");
    }
}
