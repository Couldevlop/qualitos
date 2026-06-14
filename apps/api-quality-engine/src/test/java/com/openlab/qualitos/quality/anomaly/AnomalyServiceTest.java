package com.openlab.qualitos.quality.anomaly;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyServiceTest {

    @Mock AiGatewayClient ai;
    @InjectMocks AnomalyService service;

    @Test
    void detect_mapsFullGatewayResponse() {
        when(ai.detectAnomaly(any(), eq("isolation_forest"), eq(0.1), isNull())).thenReturn(Map.of(
                "n", 3,
                "n_features", 2,
                "method", "isolation_forest",
                "contamination", 0.1,
                "threshold", 0.62,
                "anomaly_count", 1,
                "has_anomalies", true,
                "points", List.of(
                        Map.of("index", 0, "score", 0.40, "is_anomaly", false, "top_feature", 1),
                        Map.of("index", 1, "score", 0.45, "is_anomaly", false),
                        Map.of("index", 2, "score", 0.80, "is_anomaly", true))));

        AnomalyDto.DetectResponse r = service.detect(new AnomalyDto.DetectRequest(
                List.of(List.of(1.0, 2.0), List.of(1.1, 2.1), List.of(50.0, -50.0)),
                "isolation_forest", 0.1, null));

        assertThat(r.n()).isEqualTo(3);
        assertThat(r.nFeatures()).isEqualTo(2);
        assertThat(r.method()).isEqualTo("isolation_forest");
        assertThat(r.threshold()).isEqualTo(0.62);
        assertThat(r.anomalyCount()).isEqualTo(1);
        assertThat(r.hasAnomalies()).isTrue();
        assertThat(r.points()).hasSize(3);
        assertThat(r.points().get(0).topFeature()).isEqualTo(1);
        assertThat(r.points().get(1).topFeature()).isNull(); // absent → null
        assertThat(r.points().get(2).isAnomaly()).isTrue();
    }

    @Test
    void detect_toleratesMissingOrMistypedFields() {
        when(ai.detectAnomaly(any(), isNull(), isNull(), isNull())).thenReturn(Map.of(
                "points", "not-a-list"));

        AnomalyDto.DetectResponse r = service.detect(new AnomalyDto.DetectRequest(
                List.of(List.of(5.0)), null, null, null));

        assertThat(r.n()).isZero();
        assertThat(r.method()).isEmpty();
        assertThat(r.hasAnomalies()).isFalse();
        assertThat(r.points()).isEmpty();
    }

    @Test
    void detect_passesOptionalsThrough() {
        when(ai.detectAnomaly(any(), eq("reconstruction"), eq(0.2), eq(0.7))).thenReturn(Map.of());

        service.detect(new AnomalyDto.DetectRequest(
                List.of(List.of(1.0, 2.0), List.of(3.0, 4.0)), "reconstruction", 0.2, 0.7));

        ArgumentCaptor<String> method = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Double> cont = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> thr = ArgumentCaptor.forClass(Double.class);
        verify(ai).detectAnomaly(any(), method.capture(), cont.capture(), thr.capture());
        assertThat(method.getValue()).isEqualTo("reconstruction");
        assertThat(cont.getValue()).isEqualTo(0.2);
        assertThat(thr.getValue()).isEqualTo(0.7);
    }
}
