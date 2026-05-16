package com.openlab.qualitos.quality.change;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import java.time.LocalDate;
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
@WebMvcTest(controllers = ChangeRequestController.class)
class ChangeRequestControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ChangeRequestService service;
    ObjectMapper om;

    static final UUID CHG = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID APPROVER = UUID.randomUUID();
    static final UUID IMP = UUID.randomUUID();
    static final UUID TARGET = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(changeResp())));
        mockMvc.perform(get("/api/v1/changes"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void create_201() throws Exception {
        when(service.create(any())).thenReturn(changeResp());
        ChangeDto.CreateChangeRequest req = new ChangeDto.CreateChangeRequest(
                "CR-1", "Update QMS", null, ChangeRequestType.DOCUMENT, null,
                USER, null, null, null, null);
        mockMvc.perform(post("/api/v1/changes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_invalidCode_400() throws Exception {
        String body = "{\"code\":\"bad code!\",\"title\":\"t\",\"type\":\"PROCESS\","
                + "\"requesterUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/changes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingType_400() throws Exception {
        String body = "{\"code\":\"CR-1\",\"title\":\"t\",\"requesterUserId\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/changes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(CHG)).thenThrow(new ChangeRequestNotFoundException(CHG));
        mockMvc.perform(get("/api/v1/changes/{id}", CHG))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_patches() throws Exception {
        when(service.update(any(), any())).thenReturn(changeResp());
        mockMvc.perform(patch("/api/v1/changes/{id}", CHG).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"renamed\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/changes/{id}", CHG).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).delete(CHG);
    }

    @Test @WithMockUser
    void submit_409_whenInvalidState() throws Exception {
        when(service.submit(CHG)).thenThrow(new ChangeStateException("no approvers"));
        mockMvc.perform(post("/api/v1/changes/{id}/submit", CHG).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void submit_200() throws Exception {
        when(service.submit(CHG)).thenReturn(changeResp());
        mockMvc.perform(post("/api/v1/changes/{id}/submit", CHG).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void cancel_200_withReasonParam() throws Exception {
        when(service.cancel(eq(CHG), eq("dup"))).thenReturn(changeResp());
        mockMvc.perform(post("/api/v1/changes/{id}/cancel", CHG).param("reason", "dup")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void implement_200() throws Exception {
        when(service.implement(eq(CHG), any())).thenReturn(changeResp());
        mockMvc.perform(post("/api/v1/changes/{id}/implement", CHG).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"implementedAt\":\"2026-06-01\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void implement_missingDate_400() throws Exception {
        mockMvc.perform(post("/api/v1/changes/{id}/implement", CHG).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void summary_200() throws Exception {
        when(service.summary(CHG)).thenReturn(new ChangeDto.ChangeSummary(
                CHG, ChangeRequestStatus.SUBMITTED, 2L, 0L, 0L, 2L, 0, List.of(), List.of()));
        mockMvc.perform(get("/api/v1/changes/{id}/summary", CHG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApprovers").value(2));
    }

    // --- approvers / decisions ---

    @Test @WithMockUser
    void addApprover_201() throws Exception {
        when(service.addApprover(eq(CHG), any())).thenReturn(approvalResp());
        ChangeDto.AddApproverRequest req = new ChangeDto.AddApproverRequest(APPROVER, 1);
        mockMvc.perform(post("/api/v1/changes/{id}/approvers", CHG).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void addApprover_missingUserId_400() throws Exception {
        mockMvc.perform(post("/api/v1/changes/{id}/approvers", CHG).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void removeApprover_204() throws Exception {
        mockMvc.perform(delete("/api/v1/changes/{id}/approvers/{u}", CHG, APPROVER).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).removeApprover(CHG, APPROVER);
    }

    @Test @WithMockUser
    void decide_200() throws Exception {
        when(service.decide(eq(CHG), any())).thenReturn(approvalResp());
        ChangeDto.DecisionRequest req = new ChangeDto.DecisionRequest(
                APPROVER, ApprovalDecision.APPROVED, "ok");
        mockMvc.perform(post("/api/v1/changes/{id}/decisions", CHG).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void decide_pendingValue_propagated409() throws Exception {
        when(service.decide(eq(CHG), any()))
                .thenThrow(new ChangeStateException("PENDING not allowed"));
        ChangeDto.DecisionRequest req = new ChangeDto.DecisionRequest(
                APPROVER, ApprovalDecision.PENDING, null);
        mockMvc.perform(post("/api/v1/changes/{id}/decisions", CHG).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void decide_missingDecision_400() throws Exception {
        String body = "{\"approverUserId\":\"" + APPROVER + "\"}";
        mockMvc.perform(post("/api/v1/changes/{id}/decisions", CHG).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void listApprovals_200() throws Exception {
        when(service.listApprovals(CHG)).thenReturn(List.of(approvalResp()));
        mockMvc.perform(get("/api/v1/changes/{id}/approvals", CHG))
                .andExpect(status().isOk());
    }

    // --- impacts ---

    @Test @WithMockUser
    void addImpact_201() throws Exception {
        when(service.addImpact(eq(CHG), any())).thenReturn(impactResp());
        ChangeDto.AddImpactRequest req = new ChangeDto.AddImpactRequest(
                ChangeImpactTargetType.DOCUMENT, TARGET, "note");
        mockMvc.perform(post("/api/v1/changes/{id}/impacts", CHG).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void addImpact_missingTargetType_400() throws Exception {
        String body = "{\"targetId\":\"" + TARGET + "\"}";
        mockMvc.perform(post("/api/v1/changes/{id}/impacts", CHG).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void removeImpact_204() throws Exception {
        mockMvc.perform(delete("/api/v1/changes/{id}/impacts/{impactId}", CHG, IMP).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).removeImpact(CHG, IMP);
    }

    @Test @WithMockUser
    void removeImpact_childNotFound_404() throws Exception {
        doThrow(new ChangeChildNotFoundException("Impact", IMP))
                .when(service).removeImpact(CHG, IMP);
        mockMvc.perform(delete("/api/v1/changes/{id}/impacts/{impactId}", CHG, IMP).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void listImpacts_200() throws Exception {
        when(service.listImpacts(CHG)).thenReturn(List.of(impactResp()));
        mockMvc.perform(get("/api/v1/changes/{id}/impacts", CHG))
                .andExpect(status().isOk());
    }

    // --- factories ---

    private ChangeDto.ChangeResponse changeResp() {
        return new ChangeDto.ChangeResponse(
                CHG, TENANT, "CR-1", "Update QMS", null,
                ChangeRequestType.DOCUMENT, ChangeRequestPriority.MEDIUM,
                ChangeRequestStatus.DRAFT,
                USER, null, null, null, null, null, null,
                Instant.now(), Instant.now());
    }

    private ChangeDto.ApprovalResponse approvalResp() {
        return new ChangeDto.ApprovalResponse(
                UUID.randomUUID(), TENANT, CHG, APPROVER, 1,
                ApprovalDecision.APPROVED, "ok", Instant.now(), Instant.now());
    }

    private ChangeDto.ImpactResponse impactResp() {
        return new ChangeDto.ImpactResponse(
                IMP, TENANT, CHG, ChangeImpactTargetType.DOCUMENT, TARGET, "note", Instant.now());
    }
}
