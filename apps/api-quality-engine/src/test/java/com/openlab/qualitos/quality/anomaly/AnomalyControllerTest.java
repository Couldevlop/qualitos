package com.openlab.qualitos.quality.anomaly;

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
@WebMvcTest(controllers = AnomalyController.class)
class AnomalyControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AnomalyService anomalyService;
    private final ObjectMapper om = new ObjectMapper();

    @Test @WithMockUser
    void detect_returns200_withMappedResult() throws Exception {
        when(anomalyService.detect(any())).thenReturn(new AnomalyDto.DetectResponse(
                3, 2, "isolation_forest", 0.1, 0.62, 1, true,
                List.of(
                        new AnomalyDto.Point(0, 0.40, false, null),
                        new AnomalyDto.Point(2, 0.80, true, 1))));

        mockMvc.perform(post("/api/v1/ai/anomaly/detect").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "samples", List.of(List.of(1.0, 2.0), List.of(50.0, -50.0))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasAnomalies").value(true))
                .andExpect(jsonPath("$.anomalyCount").value(1))
                .andExpect(jsonPath("$.method").value("isolation_forest"))
                .andExpect(jsonPath("$.points[1].isAnomaly").value(true))
                .andExpect(jsonPath("$.points[1].topFeature").value(1));
    }

    @Test @WithMockUser
    void detect_emptySamples_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/anomaly/detect").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("samples", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void detect_badContamination_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/anomaly/detect").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "samples", List.of(List.of(1.0, 2.0)),
                                "contamination", 0.9))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void detect_gatewayDown_returns502() throws Exception {
        when(anomalyService.detect(any())).thenThrow(new AiGatewayException("ai-service down"));
        mockMvc.perform(post("/api/v1/ai/anomaly/detect").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "samples", List.of(List.of(1.0, 2.0))))))
                .andExpect(status().isBadGateway());
    }

    @Test @WithMockUser
    void explain_returns200_withContributions() throws Exception {
        when(anomalyService.explain(any())).thenReturn(new AnomalyDto.ExplainResponse(
                2, "isolation_forest", 0.82, 0.50,
                List.of(new AnomalyDto.Contribution(0, 50.0, 0.20),
                        new AnomalyDto.Contribution(1, -50.0, 0.12))));

        mockMvc.perform(post("/api/v1/ai/anomaly/explain").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "samples", List.of(List.of(1.0, 2.0), List.of(50.0, -50.0)),
                                "index", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("isolation_forest"))
                .andExpect(jsonPath("$.score").value(0.82))
                .andExpect(jsonPath("$.contributions[0].contribution").value(0.20));
    }

    @Test @WithMockUser
    void explain_missingIndex_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/anomaly/explain").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "samples", List.of(List.of(1.0, 2.0), List.of(3.0, 4.0))))))
                .andExpect(status().isBadRequest());
    }
}
