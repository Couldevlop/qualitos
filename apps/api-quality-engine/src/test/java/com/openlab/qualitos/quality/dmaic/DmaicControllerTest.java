package com.openlab.qualitos.quality.dmaic;

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

@WebMvcTest(controllers = DmaicController.class)
class DmaicControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DmaicService service;
    ObjectMapper om;

    static final UUID PROJECT = UUID.randomUUID();
    static final UUID MEASURE = UUID.randomUUID();
    static final UUID DEVICE = UUID.randomUUID();
    static final UUID ASSIGNMENT = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID BB = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void listProjects_returns200() throws Exception {
        when(service.listProjects(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(projectResp(DmaicStatus.ACTIVE, DmaicPhase.DEFINE))));
        mockMvc.perform(get("/api/v1/dmaic/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(PROJECT.toString()));
    }

    @Test @WithMockUser
    void listProjects_withFilters() throws Exception {
        when(service.listProjects(eq(DmaicStatus.ACTIVE), eq(DmaicPhase.IMPROVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api/v1/dmaic/projects").param("status", "ACTIVE").param("phase", "IMPROVE"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void createProject_returns201() throws Exception {
        when(service.createProject(any())).thenReturn(projectResp(DmaicStatus.ACTIVE, DmaicPhase.DEFINE));
        DmaicDto.CreateProjectRequest req = new DmaicDto.CreateProjectRequest(
                "P1", "pb", "goal", BB, null, null, 10.0, 20.0, 15.0, "mm", 50000.0);
        mockMvc.perform(post("/api/v1/dmaic/projects").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void createProject_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/dmaic/projects").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"blackBeltId\":\"" + BB + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void createProject_missingBlackBelt_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/dmaic/projects").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"P\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void getProject_found() throws Exception {
        when(service.getProject(PROJECT)).thenReturn(projectResp(DmaicStatus.ACTIVE, DmaicPhase.DEFINE));
        mockMvc.perform(get("/api/v1/dmaic/projects/{id}", PROJECT)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getProject_notFound() throws Exception {
        when(service.getProject(PROJECT)).thenThrow(new DmaicProjectNotFoundException(PROJECT));
        mockMvc.perform(get("/api/v1/dmaic/projects/{id}", PROJECT)).andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_success() throws Exception {
        when(service.updateProject(eq(PROJECT), any())).thenReturn(projectResp(DmaicStatus.ACTIVE, DmaicPhase.DEFINE));
        mockMvc.perform(patch("/api/v1/dmaic/projects/{id}", PROJECT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"X\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void advance_success() throws Exception {
        when(service.advancePhase(PROJECT)).thenReturn(projectResp(DmaicStatus.ACTIVE, DmaicPhase.MEASURE));
        mockMvc.perform(patch("/api/v1/dmaic/projects/{id}/advance", PROJECT).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("MEASURE"));
    }

    @Test @WithMockUser
    void advance_invalid_returns409() throws Exception {
        when(service.advancePhase(PROJECT)).thenThrow(new DmaicStateException("nope"));
        mockMvc.perform(patch("/api/v1/dmaic/projects/{id}/advance", PROJECT).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void hold_success() throws Exception {
        when(service.hold(PROJECT)).thenReturn(projectResp(DmaicStatus.ON_HOLD, DmaicPhase.MEASURE));
        mockMvc.perform(patch("/api/v1/dmaic/projects/{id}/hold", PROJECT).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void resume_success() throws Exception {
        when(service.resume(PROJECT)).thenReturn(projectResp(DmaicStatus.ACTIVE, DmaicPhase.MEASURE));
        mockMvc.perform(patch("/api/v1/dmaic/projects/{id}/resume", PROJECT).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void cancel_success() throws Exception {
        when(service.cancel(PROJECT)).thenReturn(projectResp(DmaicStatus.CANCELLED, DmaicPhase.MEASURE));
        mockMvc.perform(patch("/api/v1/dmaic/projects/{id}/cancel", PROJECT).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_success() throws Exception {
        doNothing().when(service).deleteProject(PROJECT);
        mockMvc.perform(delete("/api/v1/dmaic/projects/{id}", PROJECT).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void delete_completed_returns409() throws Exception {
        doThrow(new DmaicStateException("c")).when(service).deleteProject(PROJECT);
        mockMvc.perform(delete("/api/v1/dmaic/projects/{id}", PROJECT).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void addMeasure_returns201() throws Exception {
        when(service.addMeasure(eq(PROJECT), any())).thenReturn(measureResp());
        mockMvc.perform(post("/api/v1/dmaic/projects/{id}/measures", PROJECT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":15.4}"))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void addMeasure_missingValue_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/dmaic/projects/{id}/measures", PROJECT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void deleteMeasure_success() throws Exception {
        doNothing().when(service).deleteMeasure(PROJECT, MEASURE);
        mockMvc.perform(delete("/api/v1/dmaic/projects/{id}/measures/{mid}", PROJECT, MEASURE).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void deleteMeasure_notFound_returns404() throws Exception {
        doThrow(new ProcessMeasureNotFoundException(MEASURE)).when(service).deleteMeasure(PROJECT, MEASURE);
        mockMvc.perform(delete("/api/v1/dmaic/projects/{id}/measures/{mid}", PROJECT, MEASURE).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void capability_returns200() throws Exception {
        when(service.computeCapability(PROJECT)).thenReturn(new DmaicDto.CapabilityResponse(
                50, 15.0, 1.0, 12.0, 18.0, 10.0, 20.0, 15.0,
                1.67, 1.67, 1.67, 1.67, 5.0, "Très capable", List.of()));
        mockMvc.perform(get("/api/v1/dmaic/projects/{id}/capability", PROJECT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpk").value(1.67))
                .andExpect(jsonPath("$.sampleSize").value(50));
    }

    @Test @WithMockUser
    void listDevices_returns200() throws Exception {
        when(service.listDevices(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deviceSummary())));
        mockMvc.perform(get("/api/v1/dmaic/pokayoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].code").value("PY-X"));
    }

    @Test @WithMockUser
    void getDevice_found() throws Exception {
        when(service.getDevice(DEVICE)).thenReturn(deviceDetail());
        mockMvc.perform(get("/api/v1/dmaic/pokayoke/{id}", DEVICE)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void getDevice_notFound() throws Exception {
        when(service.getDevice(DEVICE)).thenThrow(new PokaYokeDeviceNotFoundException(DEVICE));
        mockMvc.perform(get("/api/v1/dmaic/pokayoke/{id}", DEVICE)).andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void getDeviceByCode_found() throws Exception {
        when(service.getDeviceByCode("PY-X")).thenReturn(deviceDetail());
        mockMvc.perform(get("/api/v1/dmaic/pokayoke/by-code/{code}", "PY-X")).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void assign_returns201() throws Exception {
        when(service.assignDevice(eq(PROJECT), any())).thenReturn(assignmentResp());
        mockMvc.perform(post("/api/v1/dmaic/projects/{id}/pokayoke", PROJECT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"" + DEVICE + "\"}"))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void assign_missingDeviceId_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/dmaic/projects/{id}/pokayoke", PROJECT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void updateAssignment_success() throws Exception {
        when(service.updateAssignment(eq(PROJECT), eq(ASSIGNMENT), any())).thenReturn(assignmentResp());
        mockMvc.perform(patch("/api/v1/dmaic/projects/{id}/pokayoke/{aid}", PROJECT, ASSIGNMENT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_DESIGN\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void updateAssignment_invalidTransition_returns409() throws Exception {
        when(service.updateAssignment(eq(PROJECT), eq(ASSIGNMENT), any()))
                .thenThrow(new DmaicStateException("bad transition"));
        mockMvc.perform(patch("/api/v1/dmaic/projects/{id}/pokayoke/{aid}", PROJECT, ASSIGNMENT).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VERIFIED\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void deleteAssignment_success() throws Exception {
        doNothing().when(service).deleteAssignment(PROJECT, ASSIGNMENT);
        mockMvc.perform(delete("/api/v1/dmaic/projects/{id}/pokayoke/{aid}", PROJECT, ASSIGNMENT).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void deleteAssignment_verified_returns409() throws Exception {
        doThrow(new DmaicStateException("verified")).when(service).deleteAssignment(PROJECT, ASSIGNMENT);
        mockMvc.perform(delete("/api/v1/dmaic/projects/{id}/pokayoke/{aid}", PROJECT, ASSIGNMENT).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void create_missingTenant_returns403() throws Exception {
        when(service.createProject(any())).thenThrow(new MissingTenantContextException());
        DmaicDto.CreateProjectRequest req = new DmaicDto.CreateProjectRequest(
                "P", null, null, BB, null, null, null, null, null, null, null);
        mockMvc.perform(post("/api/v1/dmaic/projects").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // helpers
    private DmaicDto.ProjectResponse projectResp(DmaicStatus s, DmaicPhase p) {
        return new DmaicDto.ProjectResponse(
                PROJECT, TENANT, "P1", "pb", "goal", p, s, null, BB, null,
                10.0, 20.0, 15.0, "mm", 50000.0,
                0, 0, Instant.now(), null, Instant.now(), Instant.now());
    }

    private DmaicDto.MeasureResponse measureResp() {
        return new DmaicDto.MeasureResponse(MEASURE, PROJECT, 15.4, null, null,
                Instant.now(), null, null, Instant.now());
    }

    private DmaicDto.DeviceSummary deviceSummary() {
        return new DmaicDto.DeviceSummary(DEVICE, "PY-X", "Device X",
                PokaYokeType.PREVENTION, PokaYokeMechanism.SENSOR, "all", "low");
    }

    private DmaicDto.DeviceDetail deviceDetail() {
        return new DmaicDto.DeviceDetail(DEVICE, "PY-X", "Device X", "desc",
                PokaYokeType.PREVENTION, PokaYokeMechanism.SENSOR,
                "all", "ex", "low", Instant.now(), Instant.now());
    }

    private DmaicDto.AssignmentResponse assignmentResp() {
        return new DmaicDto.AssignmentResponse(ASSIGNMENT, PROJECT, DEVICE, "PY-X", "Device X",
                PokaYokeType.PREVENTION, PokaYokeAssignmentStatus.PROPOSED, null,
                null, null, null, Instant.now(), Instant.now());
    }
}
