package com.openlab.qualitos.quality.spc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.aigateway.AiGatewayException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("web")
@WebMvcTest(controllers = SpcController.class)
class SpcControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SpcService spcService;
    private final ObjectMapper om = new ObjectMapper();

    @Test @WithMockUser
    void analyze_returns200_withMappedResult() throws Exception {
        when(spcService.analyze(any())).thenReturn(new SpcDto.AnalyzeResponse(
                3, true,
                new SpcDto.Limits(2.0, 0.5, 3.5, 0.5, true),
                List.of(new SpcDto.Violation("RULE_1", "Hors 3σ", "1 point au-delà",
                        List.of(2), "CRITICAL"))));

        mockMvc.perform(post("/api/v1/ai/spc/analyze").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("values", List.of(1.0, 2.0, 3.0)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outOfControl").value(true))
                .andExpect(jsonPath("$.limits.ucl").value(3.5))
                .andExpect(jsonPath("$.violations[0].rule").value("RULE_1"))
                .andExpect(jsonPath("$.violations[0].pointIndices[0]").value(2));
    }

    @Test @WithMockUser
    void analyze_emptyValues_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/spc/analyze").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("values", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void analyze_gatewayDown_returns502() throws Exception {
        when(spcService.analyze(any())).thenThrow(new AiGatewayException("ai-service down"));
        mockMvc.perform(post("/api/v1/ai/spc/analyze").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("values", List.of(1.0, 2.0)))))
                .andExpect(status().isBadGateway());
    }

    // ---- prévision de sortie des limites (§6.5) ----

    @Test @WithMockUser
    void forecast_returns200_withMappedResult() throws Exception {
        when(spcService.forecast(any())).thenReturn(prevision(true, 4, "upper"));

        mockMvc.perform(post("/api/v1/ai/spc/forecast").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "values", List.of(10.0, 10.6, 11.2, 11.8), "horizon", 12))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.willBreach").value(true))
                .andExpect(jsonPath("$.firstBreachStep").value(4))
                .andExpect(jsonPath("$.side").value("upper"))
                .andExpect(jsonPath("$.confidence").value("high"))
                .andExpect(jsonPath("$.points[0].pBreach").value(0.2));
    }

    /** Trois points ne datent pas une dérive : la requête est refusée à l'entrée. */
    @Test @WithMockUser
    void forecast_seriesTooShort_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/spc/forecast").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("values", List.of(1.0, 2.0, 3.0)))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void forecast_horizonOutOfBounds_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/spc/forecast").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "values", List.of(10.0, 10.6, 11.2, 11.8), "horizon", 61))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void forecast_thresholdOutOfBounds_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/spc/forecast").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "values", List.of(10.0, 10.6, 11.2, 11.8), "threshold", 1.4))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void forecast_gatewayDown_returns502() throws Exception {
        when(spcService.forecast(any())).thenThrow(new AiGatewayException("ai-service down"));

        mockMvc.perform(post("/api/v1/ai/spc/forecast").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "values", List.of(10.0, 10.6, 11.2, 11.8)))))
                .andExpect(status().isBadGateway());
    }

    @Test @WithMockUser
    void forecastKpi_returns200_withSeriesAndForecast() throws Exception {
        UUID kpi = UUID.randomUUID();
        when(spcService.forecastKpi(eq(kpi), eq(30), isNull(), eq(false)))
                .thenReturn(new SpcDto.KpiForecastResponse(
                        kpi, "OEE", "Rendement", "%",
                        List.of("2026-11-01T00:00:00Z"), List.of(10.0),
                        prevision(true, 2, "lower"), null));

        mockMvc.perform(post("/api/v1/ai/spc/kpi/" + kpi + "/forecast").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpiCode").value("OEE"))
                .andExpect(jsonPath("$.forecast.side").value("lower"))
                .andExpect(jsonPath("$.capaId").doesNotExist());
    }

    @Test @WithMockUser
    void forecastKpi_openCapa_passesTheFlagThrough() throws Exception {
        UUID kpi = UUID.randomUUID();
        UUID capa = UUID.randomUUID();
        when(spcService.forecastKpi(eq(kpi), eq(10), eq(8), eq(true)))
                .thenReturn(new SpcDto.KpiForecastResponse(
                        kpi, "OEE", "Rendement", "%", List.of(), List.of(),
                        prevision(true, 2, "upper"), capa));

        mockMvc.perform(post("/api/v1/ai/spc/kpi/" + kpi + "/forecast").with(csrf())
                        .param("limit", "10").param("horizon", "8").param("openCapa", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capaId").value(capa.toString()));
    }

    private static SpcDto.ForecastResponse prevision(boolean sortie, Integer etape, String cote) {
        return new SpcDto.ForecastResponse(
                4, 12, 0.5, sortie, etape, cote, 0.4, 0.2, 0.95, "high", "holt_linear",
                new SpcDto.Limits(10.0, 1.0, 13.0, 7.0, true),
                List.of(new SpcDto.ForecastPoint(1, 12.0, 10.0, 14.0, 0.2, 0.0, 0.2),
                        new SpcDto.ForecastPoint(2, 13.5, 11.0, 16.0, 0.8, 0.0, 0.8)));
    }
}
