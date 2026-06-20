package com.openlab.qualitos.quality.dashboards.timetravel.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.dashboards.timetravel.application.TimeTravelDto;
import com.openlab.qualitos.quality.dashboards.timetravel.application.TimeTravelService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = TimeTravelController.class)
@Import(MethodSecurityTestConfig.class)
class TimeTravelControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean TimeTravelService service;

    static final UUID KPI = UUID.randomUUID();
    static final Instant ASOF = Instant.parse("2026-03-15T00:00:00Z");

    @Test @WithMockUser
    void kpisAsOf_200_present() throws Exception {
        var snap = new TimeTravelDto.DashboardSnapshotView(ASOF, false, List.of(
                new TimeTravelDto.KpiSnapshotView(KPI, "fpy", "First Pass Yield", "%",
                        new BigDecimal("94.2"), Instant.parse("2026-03-01T00:00:00Z"), true)));
        when(service.snapshotAsOf(any())).thenReturn(snap);

        mockMvc.perform(get("/api/v1/dashboards/time-travel/kpis")
                        .param("asOf", "2026-03-15T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(false))
                .andExpect(jsonPath("$.kpis[0].code").value("fpy"))
                .andExpect(jsonPath("$.kpis[0].present").value(true));
    }

    @Test @WithMockUser
    void kpisAsOf_200_empty() throws Exception {
        var snap = new TimeTravelDto.DashboardSnapshotView(ASOF, true, List.of());
        when(service.snapshotAsOf(any())).thenReturn(snap);
        mockMvc.perform(get("/api/v1/dashboards/time-travel/kpis")
                        .param("asOf", "2026-03-15T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empty").value(true));
    }

    @Test @WithMockUser
    void kpisAsOf_missingParam_400() throws Exception {
        mockMvc.perform(get("/api/v1/dashboards/time-travel/kpis"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void kpisAsOf_invalidDate_400() throws Exception {
        mockMvc.perform(get("/api/v1/dashboards/time-travel/kpis").param("asOf", "not-a-date"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithAnonymousUser
    void kpisAsOf_anonymous_4xx() throws Exception {
        mockMvc.perform(get("/api/v1/dashboards/time-travel/kpis")
                        .param("asOf", "2026-03-15T00:00:00Z"))
                .andExpect(status().is4xxClientError());
    }
}
