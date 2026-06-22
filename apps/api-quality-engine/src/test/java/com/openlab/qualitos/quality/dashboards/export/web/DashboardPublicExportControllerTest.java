package com.openlab.qualitos.quality.dashboards.export.web;

import com.openlab.qualitos.quality.dashboards.export.application.DashboardExportDto;
import com.openlab.qualitos.quality.dashboards.export.application.DashboardExportService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = DashboardPublicExportController.class)
class DashboardPublicExportControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DashboardExportService service;

    @Test @WithMockUser
    void verify_validExport_returnsIntegrityFacts() throws Exception {
        when(service.verify(eq("abcDEF012345_-xy"))).thenReturn(
                new DashboardExportDto.VerificationResult(
                        true, "abcDEF012345_-xy", "a".repeat(64), "tx-1",
                        "Exec dashboard", Instant.parse("2026-06-22T10:00:00Z")));

        mockMvc.perform(get("/api/v1/dashboards/public/exports/{code}/verify", "abcDEF012345_-xy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.sha256Hex").value("a".repeat(64)))
                .andExpect(jsonPath("$.anchorTxRef").value("tx-1"))
                .andExpect(jsonPath("$.dashboardName").value("Exec dashboard"));
    }

    @Test @WithMockUser
    void verify_unknownCode_returnsInvalid() throws Exception {
        when(service.verify(eq("ZZZZZZZZZZ01234567"))).thenReturn(
                DashboardExportDto.VerificationResult.unknown("ZZZZZZZZZZ01234567"));

        mockMvc.perform(get("/api/v1/dashboards/public/exports/{code}/verify", "ZZZZZZZZZZ01234567"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.sha256Hex").doesNotExist());
    }

    @Test @WithMockUser
    void verify_malformedCode_400() throws Exception {
        mockMvc.perform(get("/api/v1/dashboards/public/exports/{code}/verify", "bad code!"))
                .andExpect(status().is4xxClientError());
    }
}
