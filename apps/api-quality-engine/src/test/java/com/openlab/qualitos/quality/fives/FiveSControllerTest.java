package com.openlab.qualitos.quality.fives;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.visiongateway.VisionDto;
import com.openlab.qualitos.quality.visiongateway.VisionGatewayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
    @MockitoBean VisionGatewayClient visionGateway;

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

    // ---- Analyse Vision CV sur audit 5S (§3.2) --------------------------------

    /** PNG minimal valide (magic bytes) pour traverser la validation d'image. */
    private static MockMultipartFile pngPart() {
        byte[] png = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01, 0x02, 0x03};
        return new MockMultipartFile("image", "zone.png", "image/png", png);
    }

    @Test @WithMockUser
    void vision_validImage_returns200() throws Exception {
        when(service.findById(AUDIT)).thenReturn(resp(FiveSAuditStatus.IN_PROGRESS));
        VisionDto.VisionAnalysis analysis = new VisionDto.VisionAnalysis(
                "sha", 1280, 720,
                new VisionDto.VisionScore(80, 75, 90, 85, 70, 80),
                List.of(new VisionDto.VisionFinding("SEITON", "Encombrement", "MEDIUM", 0.82, List.of(1, 2, 3, 4))));
        when(visionGateway.analyze(any(), any(), any())).thenReturn(analysis);

        mockMvc.perform(multipart("/api/v1/fives/audits/{id}/vision", AUDIT).file(pngPart()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score.overall").value(80))
                .andExpect(jsonPath("$.findings[0].pillar").value("SEITON"));
    }

    @Test @WithMockUser
    void vision_unknownAudit_returns404() throws Exception {
        when(service.findById(AUDIT)).thenThrow(new FiveSAuditNotFoundException(AUDIT));
        mockMvc.perform(multipart("/api/v1/fives/audits/{id}/vision", AUDIT).file(pngPart()).with(csrf()))
                .andExpect(status().isNotFound());
        verify(visionGateway, never()).analyze(any(), any(), any());
    }

    @Test @WithMockUser
    void vision_invalidImage_returns400() throws Exception {
        when(service.findById(AUDIT)).thenReturn(resp(FiveSAuditStatus.IN_PROGRESS));
        // Content-type déclaré image/png mais contenu texte → magic bytes KO → 400.
        MockMultipartFile bogus = new MockMultipartFile(
                "image", "fake.png", "image/png", "not an image".getBytes());
        mockMvc.perform(multipart("/api/v1/fives/audits/{id}/vision", AUDIT).file(bogus).with(csrf()))
                .andExpect(status().isBadRequest());
        verify(visionGateway, never()).analyze(any(), any(), any());
    }

    private FiveSDto.AuditResponse resp(FiveSAuditStatus s) {
        return new FiveSDto.AuditResponse(AUDIT, TENANT, "Z", null, s, AUDITOR,
                null, null, null, Instant.now(), Instant.now(), List.of());
    }
}
