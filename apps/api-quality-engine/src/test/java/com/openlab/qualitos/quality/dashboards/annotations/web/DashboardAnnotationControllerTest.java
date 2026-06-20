package com.openlab.qualitos.quality.dashboards.annotations.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.dashboards.annotations.application.DashboardAnnotationDto;
import com.openlab.qualitos.quality.dashboards.annotations.application.DashboardAnnotationService;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationForbiddenException;
import com.openlab.qualitos.quality.dashboards.annotations.domain.DashboardAnnotationNotFoundException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = DashboardAnnotationController.class)
@Import(MethodSecurityTestConfig.class)
class DashboardAnnotationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DashboardAnnotationService service;

    static final UUID ID = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUTHOR = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-06-20T10:00:00Z");

    private DashboardAnnotationDto.View view() {
        return new DashboardAnnotationDto.View(ID, TENANT, AUTHOR, "exec.trend",
                "Mai", "Dérive nette", NOW, true);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.listByChart("exec.trend")).thenReturn(List.of(view()));
        mockMvc.perform(get("/api/v1/dashboards/annotations").param("chartKey", "exec.trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].body").value("Dérive nette"))
                .andExpect(jsonPath("$[0].deletable").value(true));
    }

    @Test @WithMockUser
    void list_missingChartKey_400() throws Exception {
        mockMvc.perform(get("/api/v1/dashboards/annotations"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void list_invalidChartKey_400() throws Exception {
        mockMvc.perform(get("/api/v1/dashboards/annotations").param("chartKey", "BAD KEY"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithAnonymousUser
    void list_anonymous_401or403() throws Exception {
        mockMvc.perform(get("/api/v1/dashboards/annotations").param("chartKey", "exec.trend"))
                .andExpect(status().is4xxClientError());
    }

    @Test @WithMockUser
    void create_201() throws Exception {
        when(service.create(any())).thenReturn(view());
        mockMvc.perform(post("/api/v1/dashboards/annotations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chartKey\":\"exec.trend\",\"anchorLabel\":\"Mai\",\"body\":\"Dérive nette\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ID.toString()));
    }

    @Test @WithMockUser
    void create_blankBody_400() throws Exception {
        mockMvc.perform(post("/api/v1/dashboards/annotations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chartKey\":\"exec.trend\",\"body\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_invalidChartKey_400() throws Exception {
        mockMvc.perform(post("/api/v1/dashboards/annotations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chartKey\":\"BAD KEY\",\"body\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        doNothing().when(service).delete(eq(ID));
        mockMvc.perform(delete("/api/v1/dashboards/annotations/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void delete_notFound_404() throws Exception {
        doThrow(new DashboardAnnotationNotFoundException(ID)).when(service).delete(eq(ID));
        mockMvc.perform(delete("/api/v1/dashboards/annotations/{id}", ID).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void delete_forbidden_403() throws Exception {
        doThrow(new DashboardAnnotationForbiddenException(ID)).when(service).delete(eq(ID));
        mockMvc.perform(delete("/api/v1/dashboards/annotations/{id}", ID).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
