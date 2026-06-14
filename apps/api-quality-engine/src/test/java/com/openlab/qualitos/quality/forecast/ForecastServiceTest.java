package com.openlab.qualitos.quality.forecast;

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
class ForecastServiceTest {

    @Mock AiGatewayClient ai;
    @InjectMocks ForecastService service;

    @Test
    void forecast_mapsFullGatewayResponse() {
        when(ai.forecastKpi(any(), eq(35.0), eq(6), eq("at_least"), isNull())).thenReturn(Map.ofEntries(
                Map.entry("n", 10),
                Map.entry("slope", 2.0),
                Map.entry("intercept", 28.0),
                Map.entry("residual_sigma", 0.5),
                Map.entry("r2", 0.99),
                Map.entry("horizon", 6),
                Map.entry("target", 35.0),
                Map.entry("direction", "at_least"),
                Map.entry("probability", 0.97),
                Map.entry("confidence", "high"),
                Map.entry("model", "holt_linear"),
                Map.entry("seasonal_period", 0),
                Map.entry("points", List.of(
                        Map.of("step", 1, "value", 30.0, "low", 29.0, "high", 31.0),
                        Map.of("step", 6, "value", 40.0, "low", 37.0, "high", 43.0)))));

        ForecastDto.ForecastResponse r = service.forecast(new ForecastDto.ForecastRequest(
                List.of(10.0, 12.0, 14.0, 16.0), 35.0, 6, "at_least", null));

        assertThat(r.n()).isEqualTo(10);
        assertThat(r.slope()).isEqualTo(2.0);
        assertThat(r.intercept()).isEqualTo(28.0);
        assertThat(r.residualSigma()).isEqualTo(0.5);
        assertThat(r.probability()).isEqualTo(0.97);
        assertThat(r.confidence()).isEqualTo("high");
        assertThat(r.model()).isEqualTo("holt_linear");
        assertThat(r.seasonalPeriod()).isZero();
        assertThat(r.points()).hasSize(2);
        assertThat(r.points().get(1).value()).isEqualTo(40.0);
    }

    @Test
    void forecast_toleratesMissingOrMistypedFields() {
        when(ai.forecastKpi(any(), any(), any(), any(), any())).thenReturn(Map.of(
                "points", "not-a-list"));

        ForecastDto.ForecastResponse r = service.forecast(new ForecastDto.ForecastRequest(
                List.of(1.0, 2.0, 3.0, 4.0), 10.0, null, null, null));

        assertThat(r.n()).isZero();
        assertThat(r.model()).isEmpty();
        assertThat(r.points()).isEmpty();
    }

    @Test
    void forecast_passesSeasonalPeriodThrough() {
        when(ai.forecastKpi(any(), eq(30.0), eq(4), eq("at_most"), eq(4))).thenReturn(Map.of());

        service.forecast(new ForecastDto.ForecastRequest(
                List.of(10.0, 14.0, 9.0, 13.0, 11.0, 15.0, 10.0, 14.0), 30.0, 4, "at_most", 4));

        ArgumentCaptor<Integer> season = ArgumentCaptor.forClass(Integer.class);
        verify(ai).forecastKpi(any(), eq(30.0), eq(4), eq("at_most"), season.capture());
        assertThat(season.getValue()).isEqualTo(4);
    }
}
