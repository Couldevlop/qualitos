package com.openlab.qualitos.quality.blockchain.web;

import com.openlab.qualitos.quality.blockchain.application.AnchorVerificationService;
import com.openlab.qualitos.quality.blockchain.application.AnchoringDto;
import com.openlab.qualitos.quality.blockchain.application.AnchoringService;
import com.openlab.qualitos.quality.blockchain.domain.AnchorVerificationResult;
import com.openlab.qualitos.quality.common.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = AnchoringController.class)
class AnchoringControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AnchoringService service;
    @MockitoBean AnchorVerificationService verification;

    static final UUID TENANT = UUID.randomUUID();

    @BeforeEach
    void setup() { TenantContext.setTenantId(TENANT.toString()); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test @WithMockUser
    void run_200_returnsBatchResult() throws Exception {
        when(service.anchorBatch(any())).thenReturn(new AnchoringDto.AnchorBatchResult(
                TENANT, 3, "root-hex", "tx-xyz", List.of(), 1L, 3L, Instant.now()));
        mockMvc.perform(post("/api/v1/blockchain/anchor/run").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchSize").value(3))
                .andExpect(jsonPath("$.blockchainTxRef").value("tx-xyz"));
    }

    @Test @WithMockUser
    void run_noTenant_403() throws Exception {
        TenantContext.clear();
        mockMvc.perform(post("/api/v1/blockchain/anchor/run").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser
    void run_invalidBatchSize_400() throws Exception {
        mockMvc.perform(post("/api/v1/blockchain/anchor/run")
                        .param("batchSize", "0").with(csrf()))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void verify_200_returnsResult() throws Exception {
        when(verification.verify(eq(TENANT), eq("abc123")))
                .thenReturn(AnchorVerificationResult.verified("tx-1", "root-hex"));
        mockMvc.perform(get("/api/v1/blockchain/verify").param("hash", "abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"))
                .andExpect(jsonPath("$.txRef").value("tx-1"));
    }

    @Test @WithMockUser
    void verify_notAnchored_200() throws Exception {
        when(verification.verify(any(), any()))
                .thenReturn(AnchorVerificationResult.notAnchored("événement inconnu ou non ancré"));
        mockMvc.perform(get("/api/v1/blockchain/verify").param("hash", "deadbeef"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOT_ANCHORED"));
    }
}
