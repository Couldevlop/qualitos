package com.openlab.qualitos.quality.nccluster;

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
@WebMvcTest(controllers = NcClusterController.class)
class NcClusterControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean NcClusterService ncClusterService;
    private final ObjectMapper om = new ObjectMapper();

    @Test @WithMockUser
    void cluster_returns200_withMappedResult() throws Exception {
        when(ncClusterService.cluster(any())).thenReturn(new NcClusterDto.ClusterResponse(
                5, 0.8, "dbscan",
                List.of(new NcClusterDto.Cluster(0, List.of(0, 1), 2, List.of("fuite", "huile"))),
                List.of(4)));

        mockMvc.perform(post("/api/v1/ai/nc-clusters").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "texts", List.of("fuite huile presse", "fuite huile presse ligne")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("dbscan"))
                .andExpect(jsonPath("$.clusters[0].size").value(2))
                .andExpect(jsonPath("$.clusters[0].topTerms[0]").value("fuite"))
                .andExpect(jsonPath("$.noiseIndices[0]").value(4));
    }

    @Test @WithMockUser
    void cluster_tooFewTexts_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/ai/nc-clusters").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of("texts", List.of("seul texte")))))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void cluster_gatewayDown_returns502() throws Exception {
        when(ncClusterService.cluster(any())).thenThrow(new AiGatewayException("ai-service down"));
        mockMvc.perform(post("/api/v1/ai/nc-clusters").with(csrf())
                        .contentType("application/json")
                        .content(om.writeValueAsString(Map.of(
                                "texts", List.of("fuite huile", "fuite huile ligne")))))
                .andExpect(status().isBadGateway());
    }
}
