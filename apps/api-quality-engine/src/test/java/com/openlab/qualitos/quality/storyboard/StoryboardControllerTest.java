package com.openlab.qualitos.quality.storyboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.aigateway.AiGatewayException;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
@WebMvcTest(controllers = StoryboardController.class)
@Import(MethodSecurityTestConfig.class)
class StoryboardControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean StoryboardService storyboardService;
    private final ObjectMapper om = new ObjectMapper();

    private Map<String, Object> validBody() {
        return Map.of(
                "period", "Mai 2026",
                "context", "Site de Lyon",
                "points", List.of(
                        Map.of("label", "Taux de NC", "value", "1,8", "trend", "-12 %",
                                "target", "< 2", "unit", "%")));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void generate_returns200_withNarrativeAndSources() throws Exception {
        when(storyboardService.generate(any())).thenReturn(new StoryboardDto.StoryboardResponse(
                "Sur mai 2026, le taux de NC recule.", "ollama", "Mai 2026",
                List.of(new StoryboardDto.IndicatorPoint("Taux de NC", "1,8", "-12 %", "< 2", "%"))));

        mockMvc.perform(post("/api/v1/ai/storyboard").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(validBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.narrative").value("Sur mai 2026, le taux de NC recule."))
                .andExpect(jsonPath("$.provider").value("ollama"))
                .andExpect(jsonPath("$.period").value("Mai 2026"))
                .andExpect(jsonPath("$.sources[0].label").value("Taux de NC"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void generate_blankPeriod_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/storyboard").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "period", "  ",
                                "points", List.of(Map.of("label", "FPY", "value", "97"))))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void generate_emptyPoints_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/storyboard").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "period", "Mai 2026",
                                "points", List.of()))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void generate_pointWithoutValue_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/storyboard").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "period", "Mai 2026",
                                "points", List.of(Map.of("label", "FPY"))))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void generate_gatewayDown_returns502() throws Exception {
        when(storyboardService.generate(any())).thenThrow(new AiGatewayException("ai-service down"));

        mockMvc.perform(post("/api/v1/ai/storyboard").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(validBody())))
                .andExpect(status().isBadGateway());
    }

    @Test @WithMockUser(roles = "USER")
    void generate_insufficientRole_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/ai/storyboard").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(validBody())))
                .andExpect(status().isForbidden());
    }
}
