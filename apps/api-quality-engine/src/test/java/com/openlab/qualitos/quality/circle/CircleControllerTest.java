package com.openlab.qualitos.quality.circle;

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
@WebMvcTest(controllers = CircleController.class)
class CircleControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CircleService service;
    ObjectMapper om;

    static final UUID CIRCLE = UUID.randomUUID();
    static final UUID MEMBER = UUID.randomUUID();
    static final UUID MEETING = UUID.randomUUID();
    static final UUID PROPOSAL = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID VALIDATOR = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.findAll(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(circleResp(CircleStatus.ACTIVE))));
        mockMvc.perform(get("/api/v1/circles"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void list_withFilter() throws Exception {
        when(service.findAll(eq(CircleStatus.ARCHIVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(circleResp(CircleStatus.ARCHIVED))));
        mockMvc.perform(get("/api/v1/circles").param("status", "ARCHIVED"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void create_returns201() throws Exception {
        when(service.create(any())).thenReturn(circleResp(CircleStatus.ACTIVE));
        mockMvc.perform(post("/api/v1/circles").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Cercle X\"}"))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_missingName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/circles").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_found() throws Exception {
        when(service.findById(CIRCLE)).thenReturn(circleResp(CircleStatus.ACTIVE));
        mockMvc.perform(get("/api/v1/circles/{id}", CIRCLE)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void get_notFound() throws Exception {
        when(service.findById(CIRCLE)).thenThrow(new CircleNotFoundException(CIRCLE));
        mockMvc.perform(get("/api/v1/circles/{id}", CIRCLE)).andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_success() throws Exception {
        when(service.update(eq(CIRCLE), any())).thenReturn(circleResp(CircleStatus.ACTIVE));
        mockMvc.perform(patch("/api/v1/circles/{id}", CIRCLE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"X\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void pause_success() throws Exception {
        when(service.pause(CIRCLE)).thenReturn(circleResp(CircleStatus.PAUSED));
        mockMvc.perform(patch("/api/v1/circles/{id}/pause", CIRCLE).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAUSED"));
    }

    @Test @WithMockUser
    void pause_invalid_returns409() throws Exception {
        when(service.pause(CIRCLE)).thenThrow(new CircleStateException("nope"));
        mockMvc.perform(patch("/api/v1/circles/{id}/pause", CIRCLE).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void resume_success() throws Exception {
        when(service.resume(CIRCLE)).thenReturn(circleResp(CircleStatus.ACTIVE));
        mockMvc.perform(patch("/api/v1/circles/{id}/resume", CIRCLE).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void archive_success() throws Exception {
        when(service.archive(CIRCLE)).thenReturn(circleResp(CircleStatus.ARCHIVED));
        mockMvc.perform(patch("/api/v1/circles/{id}/archive", CIRCLE).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_success() throws Exception {
        doNothing().when(service).delete(CIRCLE);
        mockMvc.perform(delete("/api/v1/circles/{id}", CIRCLE).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void delete_notArchived_returns409() throws Exception {
        doThrow(new CircleStateException("c")).when(service).delete(CIRCLE);
        mockMvc.perform(delete("/api/v1/circles/{id}", CIRCLE).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void addMember_returns201() throws Exception {
        when(service.addMember(eq(CIRCLE), any())).thenReturn(memberResp());
        mockMvc.perform(post("/api/v1/circles/{id}/members", CIRCLE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + USER + "\",\"role\":\"FACILITATOR\"}"))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void addMember_missingUser_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/circles/{id}/members", CIRCLE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void addMember_duplicate_returns409() throws Exception {
        when(service.addMember(eq(CIRCLE), any())).thenThrow(new CircleStateException("dup"));
        mockMvc.perform(post("/api/v1/circles/{id}/members", CIRCLE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":\"" + USER + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void updateRole_success() throws Exception {
        when(service.updateMemberRole(eq(CIRCLE), eq(MEMBER), any())).thenReturn(memberResp());
        mockMvc.perform(patch("/api/v1/circles/{id}/members/{mid}", CIRCLE, MEMBER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"SECRETARY\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void updateRole_notFound_returns404() throws Exception {
        when(service.updateMemberRole(eq(CIRCLE), eq(MEMBER), any()))
                .thenThrow(new CircleMemberNotFoundException(MEMBER));
        mockMvc.perform(patch("/api/v1/circles/{id}/members/{mid}", CIRCLE, MEMBER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"MEMBER\"}"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void removeMember_success() throws Exception {
        doNothing().when(service).removeMember(CIRCLE, MEMBER);
        mockMvc.perform(delete("/api/v1/circles/{id}/members/{mid}", CIRCLE, MEMBER).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void addMeeting_returns201() throws Exception {
        when(service.addMeeting(eq(CIRCLE), any())).thenReturn(meetingResp(MeetingStatus.PLANNED));
        CircleDto.MeetingRequest req = new CircleDto.MeetingRequest(
                "R", null, Instant.now().plusSeconds(3600), 60, "https://meet");
        mockMvc.perform(post("/api/v1/circles/{id}/meetings", CIRCLE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void addMeeting_missingFields_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/circles/{id}/meetings", CIRCLE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void hold_success() throws Exception {
        when(service.holdMeeting(eq(CIRCLE), eq(MEETING), any())).thenReturn(meetingResp(MeetingStatus.HELD));
        mockMvc.perform(patch("/api/v1/circles/{id}/meetings/{mid}/hold", CIRCLE, MEETING).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"minutes\":\"PV\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void hold_noBody_success() throws Exception {
        when(service.holdMeeting(eq(CIRCLE), eq(MEETING), any())).thenReturn(meetingResp(MeetingStatus.HELD));
        mockMvc.perform(patch("/api/v1/circles/{id}/meetings/{mid}/hold", CIRCLE, MEETING).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void cancelMeeting_success() throws Exception {
        when(service.cancelMeeting(CIRCLE, MEETING)).thenReturn(meetingResp(MeetingStatus.CANCELLED));
        mockMvc.perform(patch("/api/v1/circles/{id}/meetings/{mid}/cancel", CIRCLE, MEETING).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void cancelMeeting_held_returns409() throws Exception {
        when(service.cancelMeeting(CIRCLE, MEETING)).thenThrow(new CircleStateException("h"));
        mockMvc.perform(patch("/api/v1/circles/{id}/meetings/{mid}/cancel", CIRCLE, MEETING).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void updateMeeting_success() throws Exception {
        when(service.updateMeeting(eq(CIRCLE), eq(MEETING), any())).thenReturn(meetingResp(MeetingStatus.PLANNED));
        mockMvc.perform(patch("/api/v1/circles/{id}/meetings/{mid}", CIRCLE, MEETING).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"X\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void addProposal_returns201() throws Exception {
        when(service.addProposal(eq(CIRCLE), any())).thenReturn(proposalResp(ProposalStatus.PROPOSED));
        CircleDto.ProposalRequest req = new CircleDto.ProposalRequest("Idée", "d", USER, null);
        mockMvc.perform(post("/api/v1/circles/{id}/proposals", CIRCLE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void addProposal_missingProposer_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/circles/{id}/proposals", CIRCLE).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void review_success() throws Exception {
        when(service.reviewProposal(CIRCLE, PROPOSAL)).thenReturn(proposalResp(ProposalStatus.UNDER_REVIEW));
        mockMvc.perform(patch("/api/v1/circles/{id}/proposals/{pid}/review", CIRCLE, PROPOSAL).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UNDER_REVIEW"));
    }

    @Test @WithMockUser
    void approve_success() throws Exception {
        when(service.approveProposal(eq(CIRCLE), eq(PROPOSAL), any()))
                .thenReturn(proposalResp(ProposalStatus.APPROVED));
        mockMvc.perform(patch("/api/v1/circles/{id}/proposals/{pid}/approve", CIRCLE, PROPOSAL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validatedBy\":\"" + VALIDATOR + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void approve_missingValidator_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/circles/{id}/proposals/{pid}/approve", CIRCLE, PROPOSAL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void reject_success() throws Exception {
        when(service.rejectProposal(eq(CIRCLE), eq(PROPOSAL), any()))
                .thenReturn(proposalResp(ProposalStatus.REJECTED));
        mockMvc.perform(patch("/api/v1/circles/{id}/proposals/{pid}/reject", CIRCLE, PROPOSAL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"validatedBy\":\"" + VALIDATOR + "\",\"reason\":\"r\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void implement_success() throws Exception {
        when(service.markImplemented(CIRCLE, PROPOSAL)).thenReturn(proposalResp(ProposalStatus.IMPLEMENTED));
        mockMvc.perform(patch("/api/v1/circles/{id}/proposals/{pid}/implement", CIRCLE, PROPOSAL).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void impact_success() throws Exception {
        when(service.recordImpact(eq(CIRCLE), eq(PROPOSAL), any()))
                .thenReturn(proposalResp(ProposalStatus.MEASURED));
        mockMvc.perform(patch("/api/v1/circles/{id}/proposals/{pid}/impact", CIRCLE, PROPOSAL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"impactNote\":\"-10%\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void impact_missing_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/circles/{id}/proposals/{pid}/impact", CIRCLE, PROPOSAL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void deleteProposal_success() throws Exception {
        doNothing().when(service).deleteProposal(CIRCLE, PROPOSAL);
        mockMvc.perform(delete("/api/v1/circles/{id}/proposals/{pid}", CIRCLE, PROPOSAL).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser
    void deleteProposal_notFound_returns404() throws Exception {
        doThrow(new CircleProposalNotFoundException(PROPOSAL))
                .when(service).deleteProposal(CIRCLE, PROPOSAL);
        mockMvc.perform(delete("/api/v1/circles/{id}/proposals/{pid}", CIRCLE, PROPOSAL).with(csrf()))
                .andExpect(status().isNotFound());
    }

    // --- generateMinutes (ANO-010) ---

    @Test @WithMockUser
    void generateMinutes_returns200() throws Exception {
        CircleDto.MeetingMinutes minutes = new CircleDto.MeetingMinutes(
                "Résumé de la réunion.", List.of("Décision 1"),
                List.of(new CircleDto.ExtractedAction("Action 1", "Animateur")));
        when(service.generateMinutes(eq(CIRCLE), eq(MEETING), any())).thenReturn(minutes);
        mockMvc.perform(post("/api/v1/circles/{id}/meetings/{mid}/minutes/generate", CIRCLE, MEETING)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transcript\":\"Discussion qualité produit X. Décision : revoir le processus.\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Résumé de la réunion."))
                .andExpect(jsonPath("$.decisions[0]").value("Décision 1"))
                .andExpect(jsonPath("$.actions[0].label").value("Action 1"));
    }

    @Test @WithMockUser
    void generateMinutes_emptyTranscript_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/circles/{id}/meetings/{mid}/minutes/generate", CIRCLE, MEETING)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transcript\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void generateMinutes_meetingNotFound_returns404() throws Exception {
        when(service.generateMinutes(eq(CIRCLE), eq(MEETING), any()))
                .thenThrow(new CircleMeetingNotFoundException(MEETING));
        mockMvc.perform(post("/api/v1/circles/{id}/meetings/{mid}/minutes/generate", CIRCLE, MEETING)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transcript\":\"transcript texte\"}"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void create_missingTenant_returns403() throws Exception {
        when(service.create(any())).thenThrow(new MissingTenantContextException());
        mockMvc.perform(post("/api/v1/circles").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"C\"}"))
                .andExpect(status().isForbidden());
    }

    // helpers
    private CircleDto.CircleResponse circleResp(CircleStatus status) {
        return new CircleDto.CircleResponse(
                CIRCLE, TENANT, "Cercle", null, null, status, 0,
                Instant.now(), Instant.now(), List.of(), List.of(), List.of());
    }

    private CircleDto.MemberResponse memberResp() {
        return new CircleDto.MemberResponse(MEMBER, CIRCLE, USER, CircleRole.MEMBER, Instant.now());
    }

    private CircleDto.MeetingResponse meetingResp(MeetingStatus status) {
        return new CircleDto.MeetingResponse(
                MEETING, CIRCLE, "R", null, Instant.now(), 60, null,
                status, null, null, Instant.now(), Instant.now(), null, null);
    }

    private CircleDto.ProposalResponse proposalResp(ProposalStatus status) {
        return new CircleDto.ProposalResponse(
                PROPOSAL, CIRCLE, null, "Idée", null, status, USER, null, null, null, null, null, null,
                Instant.now(), Instant.now());
    }
}
