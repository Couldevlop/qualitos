package com.openlab.qualitos.quality.dashboards.export.web;

import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.dashboards.export.application.DashboardExportDto;
import com.openlab.qualitos.quality.dashboards.export.application.DashboardExportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = DashboardExportController.class)
class DashboardExportControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DashboardExportService service;

    static final UUID TENANT = UUID.randomUUID();
    static final UUID DASH = UUID.randomUUID();

    @BeforeEach
    void setup() { TenantContext.setTenantId(TENANT.toString()); }

    @AfterEach
    void tearDown() { TenantContext.clear(); }

    @Test @WithMockUser
    void export_200_streamsPdfWithIntegrityHeaders() throws Exception {
        byte[] pdf = "%PDF-1.7 body".getBytes();
        when(service.export(eq(DASH), any())).thenReturn(new DashboardExportDto.ExportResult(
                pdf, "dashboard-exec-20260622-100000.pdf",
                "abcDEF012345_-xy", "a".repeat(64), "tx-1", Instant.now()));

        mockMvc.perform(post("/api/v1/dashboards/custom/{id}/export/pdf", DASH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"widgets\":[{\"title\":\"K\",\"type\":\"kpi\",\"dataLines\":[\"x\"]}]}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("X-Export-Verification-Code", "abcDEF012345_-xy"))
                .andExpect(header().string("X-Export-Sha256", "a".repeat(64)))
                .andExpect(header().string("X-Export-Anchor-Ref", "tx-1"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("dashboard-exec-20260622-100000.pdf")));
    }

    @Test @WithMockUser
    void export_noBody_200() throws Exception {
        when(service.export(eq(DASH), any())).thenReturn(new DashboardExportDto.ExportResult(
                "%PDF".getBytes(), "f.pdf", "abcDEF012345_-xy", "a".repeat(64), "tx", Instant.now()));
        mockMvc.perform(post("/api/v1/dashboards/custom/{id}/export/pdf", DASH).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void export_badId_400() throws Exception {
        mockMvc.perform(post("/api/v1/dashboards/custom/{id}/export/pdf", "not-a-uuid").with(csrf()))
                .andExpect(status().is4xxClientError());
    }
}
