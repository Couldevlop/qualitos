package com.openlab.qualitos.quality.ishikawa;

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

@WebMvcTest(controllers = IshikawaController.class)
class IshikawaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IshikawaService ishikawaService;

    private ObjectMapper objectMapper;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID DIAGRAM_ID = UUID.randomUUID();
    private static final UUID CAUSE_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // --- GET /diagrams ---

    @Test
    @WithMockUser
    void listDiagrams_returns200() throws Exception {
        IshikawaDto.DiagramResponse response = buildDiagramResponse(IshikawaStatus.DRAFT);
        when(ishikawaService.findAll(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/ishikawa/diagrams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(DIAGRAM_ID.toString()))
                .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
    }

    @Test
    @WithMockUser
    void listDiagrams_withStatusFilter_returns200() throws Exception {
        IshikawaDto.DiagramResponse response = buildDiagramResponse(IshikawaStatus.VALIDATED);
        when(ishikawaService.findAll(eq(IshikawaStatus.VALIDATED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/ishikawa/diagrams").param("status", "VALIDATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("VALIDATED"));
    }

    // --- POST /diagrams ---

    @Test
    @WithMockUser
    void createDiagram_validRequest_returns201() throws Exception {
        IshikawaDto.CreateDiagramRequest request = new IshikawaDto.CreateDiagramRequest(
                "Défauts soudure", "Description", IshikawaMode.SIX_M, OWNER_ID);
        when(ishikawaService.createDiagram(any())).thenReturn(buildDiagramResponse(IshikawaStatus.DRAFT));

        mockMvc.perform(post("/api/v1/ishikawa/diagrams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(DIAGRAM_ID.toString()));
    }

    @Test
    @WithMockUser
    void createDiagram_missingProblemStatement_returns400() throws Exception {
        String body = """
                {"ownerId": "%s"}
                """.formatted(OWNER_ID);

        mockMvc.perform(post("/api/v1/ishikawa/diagrams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createDiagram_missingOwner_returns400() throws Exception {
        String body = """
                {"problemStatement": "Pb"}
                """;

        mockMvc.perform(post("/api/v1/ishikawa/diagrams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- GET /diagrams/{id} ---

    @Test
    @WithMockUser
    void getDiagram_found_returns200() throws Exception {
        when(ishikawaService.findById(DIAGRAM_ID)).thenReturn(buildDiagramResponse(IshikawaStatus.DRAFT));

        mockMvc.perform(get("/api/v1/ishikawa/diagrams/{id}", DIAGRAM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(DIAGRAM_ID.toString()));
    }

    @Test
    @WithMockUser
    void getDiagram_notFound_returns404() throws Exception {
        when(ishikawaService.findById(DIAGRAM_ID))
                .thenThrow(new IshikawaDiagramNotFoundException(DIAGRAM_ID));

        mockMvc.perform(get("/api/v1/ishikawa/diagrams/{id}", DIAGRAM_ID))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /diagrams/{id} ---

    @Test
    @WithMockUser
    void updateDiagram_success_returns200() throws Exception {
        IshikawaDto.UpdateDiagramRequest request = new IshikawaDto.UpdateDiagramRequest(
                "Nouveau", null, null, IshikawaStatus.IN_REVIEW);
        when(ishikawaService.updateDiagram(eq(DIAGRAM_ID), any()))
                .thenReturn(buildDiagramResponse(IshikawaStatus.IN_REVIEW));

        mockMvc.perform(patch("/api/v1/ishikawa/diagrams/{id}", DIAGRAM_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
    }

    @Test
    @WithMockUser
    void updateDiagram_invalidTransition_returns409() throws Exception {
        when(ishikawaService.updateDiagram(eq(DIAGRAM_ID), any()))
                .thenThrow(new IshikawaStateException("Invalid status transition: DRAFT -> VALIDATED"));

        IshikawaDto.UpdateDiagramRequest request = new IshikawaDto.UpdateDiagramRequest(
                null, null, null, IshikawaStatus.VALIDATED);

        mockMvc.perform(patch("/api/v1/ishikawa/diagrams/{id}", DIAGRAM_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // --- DELETE /diagrams/{id} ---

    @Test
    @WithMockUser
    void deleteDiagram_success_returns204() throws Exception {
        doNothing().when(ishikawaService).deleteDiagram(DIAGRAM_ID);

        mockMvc.perform(delete("/api/v1/ishikawa/diagrams/{id}", DIAGRAM_ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void deleteDiagram_validated_returns409() throws Exception {
        doThrow(new IshikawaStateException("Validated diagram cannot be deleted; archive it instead"))
                .when(ishikawaService).deleteDiagram(DIAGRAM_ID);

        mockMvc.perform(delete("/api/v1/ishikawa/diagrams/{id}", DIAGRAM_ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    // --- POST /diagrams/{id}/causes ---

    @Test
    @WithMockUser
    void addCause_validRequest_returns201() throws Exception {
        IshikawaDto.CauseRequest request = new IshikawaDto.CauseRequest(
                CauseCategory.METHODS, "Procédure inadéquate", null, null, null);
        when(ishikawaService.addCause(eq(DIAGRAM_ID), any()))
                .thenReturn(buildCauseResponse(CauseCategory.METHODS, null));

        mockMvc.perform(post("/api/v1/ishikawa/diagrams/{id}/causes", DIAGRAM_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("METHODS"));
    }

    @Test
    @WithMockUser
    void addCause_missingCategory_returns400() throws Exception {
        String body = """
                {"label": "x"}
                """;

        mockMvc.perform(post("/api/v1/ishikawa/diagrams/{id}/causes", DIAGRAM_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void addCause_invalidScoreOutOfRange_returns400() throws Exception {
        String body = """
                {"category": "METHODS", "label": "x", "rootCauseScore": 2.0}
                """;

        mockMvc.perform(post("/api/v1/ishikawa/diagrams/{id}/causes", DIAGRAM_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void addCause_categoryNotAllowed_returns409() throws Exception {
        when(ishikawaService.addCause(eq(DIAGRAM_ID), any()))
                .thenThrow(new IshikawaStateException("Category MONEY is not allowed by mode SIX_M"));

        IshikawaDto.CauseRequest request = new IshikawaDto.CauseRequest(
                CauseCategory.MONEY, "Budget", null, null, null);

        mockMvc.perform(post("/api/v1/ishikawa/diagrams/{id}/causes", DIAGRAM_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void addCause_diagramNotFound_returns404() throws Exception {
        when(ishikawaService.addCause(eq(DIAGRAM_ID), any()))
                .thenThrow(new IshikawaDiagramNotFoundException(DIAGRAM_ID));

        IshikawaDto.CauseRequest request = new IshikawaDto.CauseRequest(
                CauseCategory.METHODS, "x", null, null, null);

        mockMvc.perform(post("/api/v1/ishikawa/diagrams/{id}/causes", DIAGRAM_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /diagrams/{id}/causes/{causeId} ---

    @Test
    @WithMockUser
    void updateCause_success_returns200() throws Exception {
        IshikawaDto.UpdateCauseRequest request = new IshikawaDto.UpdateCauseRequest(
                null, "Updated", null, 0.9);
        when(ishikawaService.updateCause(eq(DIAGRAM_ID), eq(CAUSE_ID), any()))
                .thenReturn(buildCauseResponse(CauseCategory.METHODS, 0.9));

        mockMvc.perform(patch("/api/v1/ishikawa/diagrams/{id}/causes/{causeId}", DIAGRAM_ID, CAUSE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void updateCause_notFound_returns404() throws Exception {
        when(ishikawaService.updateCause(eq(DIAGRAM_ID), eq(CAUSE_ID), any()))
                .thenThrow(new IshikawaCauseNotFoundException(CAUSE_ID));

        IshikawaDto.UpdateCauseRequest request = new IshikawaDto.UpdateCauseRequest(
                null, "x", null, null);

        mockMvc.perform(patch("/api/v1/ishikawa/diagrams/{id}/causes/{causeId}", DIAGRAM_ID, CAUSE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /diagrams/{id}/causes/{causeId} ---

    @Test
    @WithMockUser
    void deleteCause_success_returns204() throws Exception {
        doNothing().when(ishikawaService).deleteCause(DIAGRAM_ID, CAUSE_ID);

        mockMvc.perform(delete("/api/v1/ishikawa/diagrams/{id}/causes/{causeId}", DIAGRAM_ID, CAUSE_ID)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void deleteCause_archived_returns409() throws Exception {
        doThrow(new IshikawaStateException("Cannot delete a cause on an archived diagram"))
                .when(ishikawaService).deleteCause(DIAGRAM_ID, CAUSE_ID);

        mockMvc.perform(delete("/api/v1/ishikawa/diagrams/{id}/causes/{causeId}", DIAGRAM_ID, CAUSE_ID)
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    // --- Missing tenant (403) ---

    @Test
    @WithMockUser
    void createDiagram_missingTenant_returns403() throws Exception {
        when(ishikawaService.createDiagram(any())).thenThrow(new MissingTenantContextException());

        IshikawaDto.CreateDiagramRequest request = new IshikawaDto.CreateDiagramRequest(
                "Pb", null, null, OWNER_ID);

        mockMvc.perform(post("/api/v1/ishikawa/diagrams")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private IshikawaDto.DiagramResponse buildDiagramResponse(IshikawaStatus status) {
        return new IshikawaDto.DiagramResponse(
                DIAGRAM_ID, TENANT_ID, "Problème test", "Description",
                IshikawaMode.SIX_M, status, OWNER_ID,
                Instant.now(), Instant.now(), List.of());
    }

    private IshikawaDto.CauseResponse buildCauseResponse(CauseCategory category, Double score) {
        return new IshikawaDto.CauseResponse(
                CAUSE_ID, DIAGRAM_ID, null, category, "Cause test", "Détail",
                score, Instant.now(), Instant.now());
    }
}
