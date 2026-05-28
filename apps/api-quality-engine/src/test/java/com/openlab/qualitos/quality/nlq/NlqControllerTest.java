package com.openlab.qualitos.quality.nlq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.aigateway.AiGatewayException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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

import org.springframework.security.test.context.support.WithMockUser;

@Tag("web")
@WebMvcTest(controllers = NlqController.class)
class NlqControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean NlqService nlqService;
    private final ObjectMapper om = new ObjectMapper();

    @Test @WithMockUser
    void ask_returns200_withMappedAnswer() throws Exception {
        when(nlqService.ask(any())).thenReturn(new NlqDto.AskResponse(
                "Combien de CAPA ?",
                "SELECT count(*) FROM capa_cases WHERE tenant_id = %(tenant_id)s",
                true, List.of("capa_cases"), List.of("count"),
                List.of(Map.of("count", 5)), 1, 0.85,
                Map.of("chart_type", "kpi"), "5 CAPA."));

        mockMvc.perform(post("/api/v1/ai/nlq/ask").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("question", "Combien de CAPA ?", "maxRows", 50))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantFilterApplied").value(true))
                .andExpect(jsonPath("$.tablesUsed[0]").value("capa_cases"))
                .andExpect(jsonPath("$.rowCount").value(1))
                .andExpect(jsonPath("$.narrative").value("5 CAPA."));
    }

    @Test @WithMockUser
    void ask_blankQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/nlq/ask").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("question", "  "))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void ask_gatewayDown_returns502() throws Exception {
        when(nlqService.ask(any())).thenThrow(new AiGatewayException("ai-service down"));
        mockMvc.perform(post("/api/v1/ai/nlq/ask").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("question", "Combien de CAPA ?"))))
                .andExpect(status().isBadGateway());
    }
}
