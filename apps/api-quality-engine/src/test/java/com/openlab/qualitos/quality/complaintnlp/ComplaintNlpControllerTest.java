package com.openlab.qualitos.quality.complaintnlp;

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
@WebMvcTest(controllers = ComplaintNlpController.class)
class ComplaintNlpControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ComplaintNlpService complaintNlpService;
    private final ObjectMapper om = new ObjectMapper();

    @Test @WithMockUser
    void analyze_returns200_withInsights() throws Exception {
        when(complaintNlpService.analyze(any())).thenReturn(new ComplaintNlpDto.AnalyzeResponse(
                1, 1, List.of(new ComplaintNlpDto.Insight(0, -0.8, "negative", "securite", true))));

        mockMvc.perform(post("/api/v1/ai/complaints/analyze").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("texts", List.of("dangereux rappel")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.criticalCount").value(1))
                .andExpect(jsonPath("$.insights[0].category").value("securite"))
                .andExpect(jsonPath("$.insights[0].critical").value(true));
    }

    @Test @WithMockUser
    void analyze_emptyTexts_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/complaints/analyze").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("texts", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void analyze_gatewayDown_returns502() throws Exception {
        when(complaintNlpService.analyze(any())).thenThrow(new AiGatewayException("ai-service down"));
        mockMvc.perform(post("/api/v1/ai/complaints/analyze").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("texts", List.of("test")))))
                .andExpect(status().isBadGateway());
    }
}
