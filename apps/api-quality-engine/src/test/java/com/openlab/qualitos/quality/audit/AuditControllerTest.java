package com.openlab.qualitos.quality.audit;

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
@WebMvcTest(controllers = AuditController.class)
class AuditControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuditService service;
    ObjectMapper om;

    static final UUID PLAN = UUID.randomUUID();
    static final UUID ITEM = UUID.randomUUID();
    static final UUID FINDING = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID LEAD = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.findAll(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(planResp(AuditStatus.PLANNED))));
        mockMvc.perform(get("/api/v1/audits/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(PLAN.toString()));
    }

    @Test @WithMockUser
    void list_withFilters() throws Exception {
        when(service.findAll(eq(AuditStatus.PLANNED), eq(AuditType.INTERNAL), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(planResp(AuditStatus.PLANNED))));
        mockMvc.perform(get("/api/v1/audits/plans")
                        .param("status", "PLANNED").param("type", "INTERNAL"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void create_returns201() throws Exception {
        when(service.createPlan(any())).thenReturn(planResp(AuditStatus.PLANNED));
        AuditDto.CreatePlanRequest req = new AuditDto.CreatePlanRequest(
                "T", null, AuditType.INTERNAL, null, LEAD, null, null);
        mockMvc.perform(post("/api/v1/audits/plans").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/audits/plans").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"INTERNAL\",\"leadAuditorId\":\"" + LEAD + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/audits/plans").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"leadAuditorId\":\"" + LEAD + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_found() throws Exception {
        when(service.findById(PLAN)).thenReturn(planResp(AuditStatus.PLANNED));
        mockMvc.perform(get("/api/v1/audits/plans/{id}", PLAN)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound() throws Exception {
        when(service.findById(PLAN)).thenThrow(new AuditPlanNotFoundException(PLAN));
        mockMvc.perform(get("/api/v1/audits/plans/{id}", PLAN)).andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_success() throws Exception {
        when(service.updatePlan(eq(PLAN), any())).thenReturn(planResp(AuditStatus.PLANNED));
        mockMvc.perform(patch("/api/v1/audits/plans/{id}", PLAN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void start_success() throws Exception {
        when(service.startPlan(PLAN)).thenReturn(planResp(AuditStatus.IN_PROGRESS));
        mockMvc.perform(patch("/api/v1/audits/plans/{id}/start", PLAN).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void start_invalid_returns409() throws Exception {
        when(service.startPlan(PLAN)).thenThrow(new AuditStateException("nope"));
        mockMvc.perform(patch("/api/v1/audits/plans/{id}/start", PLAN).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void complete_success() throws Exception {
        when(service.completePlan(eq(PLAN), any())).thenReturn(planResp(AuditStatus.COMPLETED));
        mockMvc.perform(patch("/api/v1/audits/plans/{id}/complete", PLAN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportSummary\":\"ok\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void complete_noBody_success() throws Exception {
        when(service.completePlan(eq(PLAN), any())).thenReturn(planResp(AuditStatus.COMPLETED));
        mockMvc.perform(patch("/api/v1/audits/plans/{id}/complete", PLAN).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void cancel_success() throws Exception {
        when(service.cancelPlan(PLAN)).thenReturn(planResp(AuditStatus.CANCELLED));
        mockMvc.perform(patch("/api/v1/audits/plans/{id}/cancel", PLAN).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_success() throws Exception {
        doNothing().when(service).deletePlan(PLAN);
        mockMvc.perform(delete("/api/v1/audits/plans/{id}", PLAN).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void delete_completed_returns409() throws Exception {
        doThrow(new AuditStateException("c")).when(service).deletePlan(PLAN);
        mockMvc.perform(delete("/api/v1/audits/plans/{id}", PLAN).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void addItem_returns201() throws Exception {
        when(service.addChecklistItem(eq(PLAN), any())).thenReturn(itemResp());
        AuditDto.ChecklistItemRequest req = new AuditDto.ChecklistItemRequest(
                "Q?", null, null, 1, 0);
        mockMvc.perform(post("/api/v1/audits/plans/{id}/checklist", PLAN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void addItem_missingQuestion_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/audits/plans/{id}/checklist", PLAN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void updateItem_success() throws Exception {
        when(service.updateChecklistItem(eq(PLAN), eq(ITEM), any())).thenReturn(itemResp());
        mockMvc.perform(patch("/api/v1/audits/plans/{id}/checklist/{iid}", PLAN, ITEM).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weight\":3}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void respondItem_success() throws Exception {
        when(service.respondToChecklistItem(eq(PLAN), eq(ITEM), any())).thenReturn(itemResp());
        mockMvc.perform(put("/api/v1/audits/plans/{id}/checklist/{iid}/response", PLAN, ITEM).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\":\"ok\",\"conformant\":true}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void respondItem_itemNotFound_returns404() throws Exception {
        when(service.respondToChecklistItem(eq(PLAN), eq(ITEM), any()))
                .thenThrow(new AuditChecklistItemNotFoundException(ITEM));
        mockMvc.perform(put("/api/v1/audits/plans/{id}/checklist/{iid}/response", PLAN, ITEM).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void deleteItem_success() throws Exception {
        doNothing().when(service).deleteChecklistItem(PLAN, ITEM);
        mockMvc.perform(delete("/api/v1/audits/plans/{id}/checklist/{iid}", PLAN, ITEM).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void addFinding_returns201() throws Exception {
        when(service.addFinding(eq(PLAN), any())).thenReturn(findingResp(FindingType.OBSERVATION));
        AuditDto.FindingRequest req = new AuditDto.FindingRequest(
                FindingType.OBSERVATION, "d", null, null, null, null, LEAD);
        mockMvc.perform(post("/api/v1/audits/plans/{id}/findings", PLAN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void addFinding_missingType_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/audits/plans/{id}/findings", PLAN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"d\",\"raisedBy\":\"" + LEAD + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void addFinding_notInProgress_returns409() throws Exception {
        when(service.addFinding(eq(PLAN), any()))
                .thenThrow(new AuditStateException("Findings can only be raised while audit is IN_PROGRESS"));
        AuditDto.FindingRequest req = new AuditDto.FindingRequest(
                FindingType.MINOR_NC, "d", null, null, null, null, LEAD);
        mockMvc.perform(post("/api/v1/audits/plans/{id}/findings", PLAN).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void updateFinding_success() throws Exception {
        when(service.updateFinding(eq(PLAN), eq(FINDING), any()))
                .thenReturn(findingResp(FindingType.MAJOR_NC));
        mockMvc.perform(patch("/api/v1/audits/plans/{id}/findings/{fid}", PLAN, FINDING).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"MAJOR_NC\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void updateFinding_notFound_returns404() throws Exception {
        when(service.updateFinding(eq(PLAN), eq(FINDING), any()))
                .thenThrow(new AuditFindingNotFoundException(FINDING));
        mockMvc.perform(patch("/api/v1/audits/plans/{id}/findings/{fid}", PLAN, FINDING).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void deleteFinding_success() throws Exception {
        doNothing().when(service).deleteFinding(PLAN, FINDING);
        mockMvc.perform(delete("/api/v1/audits/plans/{id}/findings/{fid}", PLAN, FINDING).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void create_missingTenant_returns403() throws Exception {
        when(service.createPlan(any())).thenThrow(new MissingTenantContextException());
        AuditDto.CreatePlanRequest req = new AuditDto.CreatePlanRequest(
                "T", null, AuditType.INTERNAL, null, LEAD, null, null);
        mockMvc.perform(post("/api/v1/audits/plans").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // helpers
    private AuditDto.PlanResponse planResp(AuditStatus s) {
        return new AuditDto.PlanResponse(
                PLAN, TENANT, "T", null, AuditType.INTERNAL, s, null,
                LEAD, null, null, null, null, null,
                Instant.now(), Instant.now(), List.of(), List.of(), null);
    }

    private AuditDto.ChecklistItemResponse itemResp() {
        return new AuditDto.ChecklistItemResponse(
                ITEM, PLAN, "Q?", null, null, 1, 0, null, null,
                Instant.now(), Instant.now());
    }

    private AuditDto.FindingResponse findingResp(FindingType t) {
        return new AuditDto.FindingResponse(
                FINDING, PLAN, null, t, "d", null, null, null, LEAD,
                Instant.now(), Instant.now(), Instant.now());
    }
}
