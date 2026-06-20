package com.openlab.qualitos.quality.standards.auditblanc.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.standards.TenantStandardNotFoundException;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditDto;
import com.openlab.qualitos.quality.standards.auditblanc.application.MockAuditService;
import com.openlab.qualitos.quality.standards.auditblanc.domain.MockAuditRunNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("web")
@WebMvcTest(controllers = MockAuditController.class)
@Import({MethodSecurityTestConfig.class, MockAuditExceptionHandler.class})
class MockAuditControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean MockAuditService service;
    ObjectMapper om;

    static final UUID ADOPTION = UUID.randomUUID();
    static final UUID RUN = UUID.randomUUID();
    static final UUID STD = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final String BASE = "/api/v1/standards/adoptions/{adoptionId}/audit-blanc-ia";

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private MockAuditDto.Report report() {
        return new MockAuditDto.Report(RUN, ADOPTION, STD, "iso-9001", "ISO 9001:2015",
                16.67, 1, 1, 1, 1,
                List.of(new MockAuditDto.QuestionView("8.1", "Q ?", "raison")),
                List.of(new MockAuditDto.GapView("8.1", "Maîtrise", "MAJOR", 0d, 4, 0,
                        "Aucune preuve.", List.of())),
                List.of(new MockAuditDto.RemediationActionView("8.1", "MAJOR", "high",
                        "AUDIT", "Lever NC.")),
                "ollama", USER, Instant.now());
    }

    // ---- run (POST) ----

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void run_returns200() throws Exception {
        when(service.run(ADOPTION)).thenReturn(report());
        mockMvc.perform(post(BASE, ADOPTION).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.standardCode").value("iso-9001"))
                .andExpect(jsonPath("$.majorCount").value(1))
                .andExpect(jsonPath("$.gaps[0].criticality").value("MAJOR"))
                .andExpect(jsonPath("$.remediationPlan[0].targetModule").value("AUDIT"));
    }

    @Test @WithMockUser(roles = "AUDITOR")
    void run_auditorAllowed() throws Exception {
        when(service.run(ADOPTION)).thenReturn(report());
        mockMvc.perform(post(BASE, ADOPTION).with(csrf())).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "USER")
    void run_insufficientRole_403() throws Exception {
        mockMvc.perform(post(BASE, ADOPTION).with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    void run_unauthenticated_401() throws Exception {
        mockMvc.perform(post(BASE, ADOPTION).with(csrf())).andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void run_unknownAdoption_404() throws Exception {
        when(service.run(ADOPTION)).thenThrow(new TenantStandardNotFoundException(ADOPTION));
        mockMvc.perform(post(BASE, ADOPTION).with(csrf())).andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void run_noClauses_409() throws Exception {
        when(service.run(ADOPTION)).thenThrow(new IllegalStateException("aucune clause exploitable"));
        mockMvc.perform(post(BASE, ADOPTION).with(csrf())).andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void run_invalidInput_400() throws Exception {
        when(service.run(ADOPTION)).thenThrow(new IllegalArgumentException("bad clause"));
        mockMvc.perform(post(BASE, ADOPTION).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("bad clause"));
    }

    // ---- history / get (GET) ----

    @Test @WithMockUser
    void history_returns200() throws Exception {
        when(service.history(ADOPTION)).thenReturn(List.of(report()));
        mockMvc.perform(get(BASE, ADOPTION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].standardCode").value("iso-9001"));
    }

    @Test @WithMockUser
    void get_returns200() throws Exception {
        when(service.get(RUN)).thenReturn(report());
        mockMvc.perform(get(BASE + "/{runId}", ADOPTION, RUN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RUN.toString()));
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(RUN)).thenThrow(new MockAuditRunNotFoundException(RUN));
        mockMvc.perform(get(BASE + "/{runId}", ADOPTION, RUN))
                .andExpect(status().isNotFound());
    }

    @Test
    void get_unauthenticated_401() throws Exception {
        mockMvc.perform(get(BASE + "/{runId}", ADOPTION, RUN))
                .andExpect(status().isUnauthorized());
    }
}
