package com.openlab.qualitos.quality.forecast;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.aigateway.AiGatewayException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("web")
@WebMvcTest(controllers = ForecastController.class)
class ForecastControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ForecastService forecastService;
    private final ObjectMapper om = new ObjectMapper();

    @Test @WithMockUser
    void forecast_returns200_withMappedResult() throws Exception {
        when(forecastService.forecast(any())).thenReturn(new ForecastDto.ForecastResponse(
                10, 2.0, 28.0, 0.5, 0.99, 6, 35.0, "at_least", 0.97, "high",
                "holt_linear", 0,
                List.of(new ForecastDto.Point(1, 30.0, 29.0, 31.0),
                        new ForecastDto.Point(6, 40.0, 37.0, 43.0))));

        mockMvc.perform(post("/api/v1/ai/forecast/kpi").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "values", List.of(10.0, 12.0, 14.0, 16.0),
                                "target", 35.0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.probability").value(0.97))
                .andExpect(jsonPath("$.confidence").value("high"))
                .andExpect(jsonPath("$.model").value("holt_linear"))
                .andExpect(jsonPath("$.points[1].value").value(40.0));
    }

    @Test @WithMockUser
    void forecast_tooFewPoints_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/forecast/kpi").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "values", List.of(1.0, 2.0, 3.0),
                                "target", 10.0))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void forecast_missingTarget_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/forecast/kpi").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "values", List.of(1.0, 2.0, 3.0, 4.0)))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void forecast_gatewayDown_returns502() throws Exception {
        when(forecastService.forecast(any())).thenThrow(new AiGatewayException("ai-service down"));
        mockMvc.perform(post("/api/v1/ai/forecast/kpi").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "values", List.of(10.0, 12.0, 14.0, 16.0),
                                "target", 35.0))))
                .andExpect(status().isBadGateway());
    }
}
