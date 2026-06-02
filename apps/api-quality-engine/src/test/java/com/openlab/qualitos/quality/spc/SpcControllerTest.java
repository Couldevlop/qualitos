package com.openlab.qualitos.quality.spc;

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
}
