package com.openlab.qualitos.quality.fives;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Tag;

@Tag("web")
@WebMvcTest(controllers = FiveSController.class)
class FiveSControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FiveSService service;

    ObjectMapper om;
    static final UUID AUDIT = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID AUDITOR = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.findAll(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(resp(FiveSAuditStatus.DRAFT))));
        mockMvc.perform(get("/api/v1/fives/audits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(AUDIT.toString()));
    }

    @Test @WithMockUser
    void list_withFilter() throws Exception {
        when(service.findAll(eq(FiveSAuditStatus.COMPLETED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(resp(FiveSAuditStatus.COMPLETED))));
        mockMvc.perform(get("/api/v1/fives/audits").param("status", "COMPLETED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));
    }

    @Test @WithMockUser
    void create_returns201() throws Exception {
        when(service.createAudit(any())).thenReturn(resp(FiveSAuditStatus.DRAFT));
        FiveSDto.CreateAuditRequest req = new FiveSDto.CreateAuditRequest(
                "Atelier", null, AUDITOR, null);
        mockMvc.perform(post("/api/v1/fives/audits").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(AUDIT.toString()));
    }

    @Test @WithMockUser
    void create_missingZone_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/fives/audits").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"auditorId\":\"" + AUDITOR + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingAuditor_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/fives/audits").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zone\":\"Z\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_found() throws Exception {
        when(service.findById(AUDIT)).thenReturn(resp(FiveSAuditStatus.DRAFT));
        mockMvc.perform(get("/api/v1/fives/audits/{id}", AUDIT))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound() throws Exception {
        when(service.findById(AUDIT)).thenThrow(new FiveSAuditNotFoundException(AUDIT));
        mockMvc.perform(get("/api/v1/fives/audits/{id}", AUDIT))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_success() throws Exception {
        when(service.updateAudit(eq(AUDIT), any())).thenReturn(resp(FiveSAuditStatus.DRAFT));
        mockMvc.perform(patch("/api/v1/fives/audits/{id}", AUDIT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"zone\":\"X\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void start_success() throws Exception {
        when(service.startAudit(AUDIT)).thenReturn(resp(FiveSAuditStatus.IN_PROGRESS));
        mockMvc.perform(patch("/api/v1/fives/audits/{id}/start", AUDIT).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test @WithMockUser
    void start_invalidState_returns409() throws Exception {
        when(service.startAudit(AUDIT)).thenThrow(new FiveSStateException("nope"));
        mockMvc.perform(patch("/api/v1/fives/audits/{id}/start", AUDIT).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void complete_success() throws Exception {
        when(service.completeAudit(AUDIT)).thenReturn(resp(FiveSAuditStatus.COMPLETED));
        mockMvc.perform(patch("/api/v1/fives/audits/{id}/complete", AUDIT).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void cancel_success() throws Exception {
        when(service.cancelAudit(AUDIT)).thenReturn(resp(FiveSAuditStatus.CANCELLED));
        mockMvc.perform(patch("/api/v1/fives/audits/{id}/cancel", AUDIT).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void score_success() throws Exception {
        FiveSDto.ItemResponse item = new FiveSDto.ItemResponse(
                UUID.randomUUID(), AUDIT, FiveSPillar.SEIRI, 8, "n", null,
                Instant.now(), Instant.now());
        when(service.scorePillar(eq(AUDIT), any())).thenReturn(item);

        FiveSDto.ScoreRequest req = new FiveSDto.ScoreRequest(FiveSPillar.SEIRI, 8, "n", null);
        mockMvc.perform(put("/api/v1/fives/audits/{id}/score", AUDIT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(8));
    }

    @Test @WithMockUser
    void score_outOfRange_returns400() throws Exception {
        mockMvc.perform(put("/api/v1/fives/audits/{id}/score", AUDIT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pillar\":\"SEIRI\",\"score\":99}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void delete_success_returns204() throws Exception {
        doNothing().when(service).deleteAudit(AUDIT);
        mockMvc.perform(delete("/api/v1/fives/audits/{id}", AUDIT).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void delete_completed_returns409() throws Exception {
        doThrow(new FiveSStateException("c")).when(service).deleteAudit(AUDIT);
        mockMvc.perform(delete("/api/v1/fives/audits/{id}", AUDIT).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void create_missingTenant_returns403() throws Exception {
        when(service.createAudit(any())).thenThrow(new MissingTenantContextException());
        mockMvc.perform(post("/api/v1/fives/audits").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new FiveSDto.CreateAuditRequest(
                                "Z", null, AUDITOR, null))))
                .andExpect(status().isForbidden());
    }

    private FiveSDto.AuditResponse resp(FiveSAuditStatus s) {
        return new FiveSDto.AuditResponse(AUDIT, TENANT, "Z", null, s, AUDITOR,
                null, null, null, Instant.now(), Instant.now(), List.of());
    }
}
