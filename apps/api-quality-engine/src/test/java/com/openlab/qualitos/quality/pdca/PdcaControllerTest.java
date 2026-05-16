package com.openlab.qualitos.quality.pdca;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.openlab.qualitos.quality.common.GlobalExceptionHandler;
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
@WebMvcTest(controllers = PdcaController.class)
class PdcaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PdcaService pdcaService;

    private ObjectMapper objectMapper;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CYCLE_ID = UUID.randomUUID();
    private static final UUID STEP_ID = UUID.randomUUID();
    private static final UUID OWNER_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    // --- GET /cycles ---

    @Test
    @WithMockUser
    void listCycles_returns200() throws Exception {
        PdcaDto.CycleResponse response = buildCycleResponse(CYCLE_ID, PdcaStatus.PLAN);
        when(pdcaService.findAll(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/pdca/cycles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(CYCLE_ID.toString()))
                .andExpect(jsonPath("$.content[0].status").value("PLAN"));
    }

    @Test
    @WithMockUser
    void listCycles_withStatusFilter_returns200() throws Exception {
        PdcaDto.CycleResponse response = buildCycleResponse(CYCLE_ID, PdcaStatus.DO);
        when(pdcaService.findAll(eq(PdcaStatus.DO), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/pdca/cycles").param("status", "DO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("DO"));
    }

    // --- POST /cycles ---

    @Test
    @WithMockUser
    void createCycle_validRequest_returns201() throws Exception {
        PdcaDto.CreateCycleRequest request = new PdcaDto.CreateCycleRequest(
                "Réduction défauts", "Description", OWNER_ID);
        PdcaDto.CycleResponse response = buildCycleResponse(CYCLE_ID, PdcaStatus.PLAN);
        when(pdcaService.createCycle(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/pdca/cycles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CYCLE_ID.toString()));
    }

    @Test
    @WithMockUser
    void createCycle_missingTitle_returns400() throws Exception {
        String body = """
                {"description": "no title", "ownerId": "%s"}
                """.formatted(OWNER_ID);

        mockMvc.perform(post("/api/v1/pdca/cycles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void createCycle_missingOwnerId_returns400() throws Exception {
        String body = """
                {"title": "Cycle sans owner"}
                """;

        mockMvc.perform(post("/api/v1/pdca/cycles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // --- GET /cycles/{id} ---

    @Test
    @WithMockUser
    void getCycle_found_returns200() throws Exception {
        PdcaDto.CycleResponse response = buildCycleResponse(CYCLE_ID, PdcaStatus.PLAN);
        when(pdcaService.findById(CYCLE_ID)).thenReturn(response);

        mockMvc.perform(get("/api/v1/pdca/cycles/{id}", CYCLE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CYCLE_ID.toString()));
    }

    @Test
    @WithMockUser
    void getCycle_notFound_returns404() throws Exception {
        when(pdcaService.findById(CYCLE_ID)).thenThrow(new PdcaCycleNotFoundException(CYCLE_ID));

        mockMvc.perform(get("/api/v1/pdca/cycles/{id}", CYCLE_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void getCycle_wrongTenant_returns404() throws Exception {
        // L'isolation tenant se traduit par un 404 (ressource invisible)
        when(pdcaService.findById(CYCLE_ID)).thenThrow(new PdcaCycleNotFoundException(CYCLE_ID));

        mockMvc.perform(get("/api/v1/pdca/cycles/{id}", CYCLE_ID))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /cycles/{id}/advance ---

    @Test
    @WithMockUser
    void advanceCycle_success_returns200() throws Exception {
        PdcaDto.CycleResponse response = buildCycleResponse(CYCLE_ID, PdcaStatus.DO);
        when(pdcaService.advanceCycle(CYCLE_ID)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/pdca/cycles/{id}/advance", CYCLE_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DO"));
    }

    @Test
    @WithMockUser
    void advanceCycle_invalidTransition_returns409() throws Exception {
        when(pdcaService.advanceCycle(CYCLE_ID))
                .thenThrow(new PdcaStateException("Cycle is already completed"));

        mockMvc.perform(patch("/api/v1/pdca/cycles/{id}/advance", CYCLE_ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void advanceCycle_notFound_returns404() throws Exception {
        when(pdcaService.advanceCycle(CYCLE_ID)).thenThrow(new PdcaCycleNotFoundException(CYCLE_ID));

        mockMvc.perform(patch("/api/v1/pdca/cycles/{id}/advance", CYCLE_ID).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /cycles/{id}/cancel ---

    @Test
    @WithMockUser
    void cancelCycle_success_returns200() throws Exception {
        PdcaDto.CycleResponse response = buildCycleResponse(CYCLE_ID, PdcaStatus.CANCELLED);
        when(pdcaService.cancelCycle(CYCLE_ID)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/pdca/cycles/{id}/cancel", CYCLE_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @WithMockUser
    void cancelCycle_alreadyCompleted_returns409() throws Exception {
        when(pdcaService.cancelCycle(CYCLE_ID))
                .thenThrow(new PdcaStateException("Completed cycle cannot be cancelled"));

        mockMvc.perform(patch("/api/v1/pdca/cycles/{id}/cancel", CYCLE_ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    // --- POST /cycles/{id}/steps ---

    @Test
    @WithMockUser
    void addStep_validRequest_returns201() throws Exception {
        PdcaDto.StepRequest request = new PdcaDto.StepRequest(
                "Analyser données", "Description", PdcaPhase.PLAN, null, null, null);
        PdcaDto.StepResponse response = buildStepResponse(STEP_ID, CYCLE_ID, PdcaPhase.PLAN);
        when(pdcaService.addStep(eq(CYCLE_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/pdca/cycles/{id}/steps", CYCLE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(STEP_ID.toString()));
    }

    @Test
    @WithMockUser
    void addStep_missingTitle_returns400() throws Exception {
        String body = """
                {"phase": "PLAN"}
                """;

        mockMvc.perform(post("/api/v1/pdca/cycles/{id}/steps", CYCLE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void addStep_cycleNotFound_returns404() throws Exception {
        when(pdcaService.addStep(eq(CYCLE_ID), any()))
                .thenThrow(new PdcaCycleNotFoundException(CYCLE_ID));

        PdcaDto.StepRequest request = new PdcaDto.StepRequest(
                "Step", null, PdcaPhase.PLAN, null, null, null);

        mockMvc.perform(post("/api/v1/pdca/cycles/{id}/steps", CYCLE_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // --- PATCH /cycles/{id}/steps/{stepId} ---

    @Test
    @WithMockUser
    void updateStep_success_returns200() throws Exception {
        PdcaDto.StepRequest request = new PdcaDto.StepRequest(
                "Updated", null, PdcaPhase.PLAN, StepStatus.IN_PROGRESS, null, null);
        PdcaDto.StepResponse response = buildStepResponse(STEP_ID, CYCLE_ID, PdcaPhase.PLAN);
        when(pdcaService.updateStep(eq(CYCLE_ID), eq(STEP_ID), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/pdca/cycles/{id}/steps/{stepId}", CYCLE_ID, STEP_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser
    void updateStep_stepNotFound_returns404() throws Exception {
        when(pdcaService.updateStep(eq(CYCLE_ID), eq(STEP_ID), any()))
                .thenThrow(new PdcaStepNotFoundException(STEP_ID));

        PdcaDto.StepRequest request = new PdcaDto.StepRequest(
                "Updated", null, PdcaPhase.PLAN, null, null, null);

        mockMvc.perform(patch("/api/v1/pdca/cycles/{id}/steps/{stepId}", CYCLE_ID, STEP_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // --- DELETE /cycles/{id}/steps/{stepId} ---

    @Test
    @WithMockUser
    void deleteStep_success_returns204() throws Exception {
        doNothing().when(pdcaService).deleteStep(CYCLE_ID, STEP_ID);

        mockMvc.perform(delete("/api/v1/pdca/cycles/{id}/steps/{stepId}", CYCLE_ID, STEP_ID)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser
    void deleteStep_phaseActive_returns409() throws Exception {
        doThrow(new PdcaStateException("Cannot delete a step belonging to the active phase PLAN"))
                .when(pdcaService).deleteStep(CYCLE_ID, STEP_ID);

        mockMvc.perform(delete("/api/v1/pdca/cycles/{id}/steps/{stepId}", CYCLE_ID, STEP_ID)
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    // --- Missing tenant (403) ---

    @Test
    @WithMockUser
    void createCycle_missingTenantContext_returns403() throws Exception {
        when(pdcaService.createCycle(any())).thenThrow(new MissingTenantContextException());

        PdcaDto.CreateCycleRequest request = new PdcaDto.CreateCycleRequest(
                "Cycle", null, OWNER_ID);

        mockMvc.perform(post("/api/v1/pdca/cycles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private PdcaDto.CycleResponse buildCycleResponse(UUID id, PdcaStatus status) {
        return new PdcaDto.CycleResponse(
                id, TENANT_ID, "Test Cycle", "Description",
                status, OWNER_ID, Instant.now(), Instant.now(), null, List.of());
    }

    private PdcaDto.StepResponse buildStepResponse(UUID id, UUID cycleId, PdcaPhase phase) {
        return new PdcaDto.StepResponse(
                id, cycleId, phase, "Test Step", "Description",
                StepStatus.PENDING, null, null, Instant.now(), Instant.now());
    }
}
