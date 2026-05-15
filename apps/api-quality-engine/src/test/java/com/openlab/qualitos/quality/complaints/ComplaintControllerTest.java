package com.openlab.qualitos.quality.complaints;

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
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ComplaintController.class)
class ComplaintControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ComplaintService service;
    ObjectMapper om;

    static final UUID CMP = UUID.randomUUID();
    static final UUID RESP = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_200() throws Exception {
        when(service.list(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(complaintResp())));
        mockMvc.perform(get("/api/v1/complaints"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void create_201() throws Exception {
        when(service.create(any())).thenReturn(complaintResp());
        ComplaintDto.CreateComplaintRequest req = new ComplaintDto.CreateComplaintRequest(
                "C-1", ComplaintChannel.EMAIL, "Alice", "alice@example.test", null,
                "Slow delivery", null, null, null, null, null, USER, null);
        mockMvc.perform(post("/api/v1/complaints").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void create_invalidCode_400() throws Exception {
        String body = "{\"code\":\"bad code!\",\"channel\":\"EMAIL\",\"subject\":\"s\","
                + "\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/complaints").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_invalidEmail_400() throws Exception {
        String body = "{\"code\":\"C-1\",\"channel\":\"EMAIL\",\"subject\":\"s\","
                + "\"customerEmail\":\"not-an-email\",\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/complaints").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_missingChannel_400() throws Exception {
        String body = "{\"code\":\"C-1\",\"subject\":\"s\",\"createdBy\":\"" + USER + "\"}";
        mockMvc.perform(post("/api/v1/complaints").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void get_notFound_404() throws Exception {
        when(service.get(CMP)).thenThrow(new ComplaintNotFoundException(CMP));
        mockMvc.perform(get("/api/v1/complaints/{id}", CMP))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void update_200() throws Exception {
        when(service.update(any(), any())).thenReturn(complaintResp());
        mockMvc.perform(patch("/api/v1/complaints/{id}", CMP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subject\":\"renamed\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void delete_204() throws Exception {
        mockMvc.perform(delete("/api/v1/complaints/{id}", CMP).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).delete(CMP);
    }

    @Test @WithMockUser
    void assign_200() throws Exception {
        when(service.assign(eq(CMP), any())).thenReturn(complaintResp());
        ComplaintDto.AssignRequest req = new ComplaintDto.AssignRequest(USER);
        mockMvc.perform(post("/api/v1/complaints/{id}/assign", CMP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void assign_missingAssignee_400() throws Exception {
        mockMvc.perform(post("/api/v1/complaints/{id}/assign", CMP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void reject_200() throws Exception {
        when(service.reject(eq(CMP), any())).thenReturn(complaintResp());
        mockMvc.perform(post("/api/v1/complaints/{id}/reject", CMP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"spam\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void reject_invalidTransition_409() throws Exception {
        when(service.reject(eq(CMP), any())).thenThrow(new ComplaintStateException("nope"));
        mockMvc.perform(post("/api/v1/complaints/{id}/reject", CMP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"r\"}"))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void resolve_200_noBody() throws Exception {
        when(service.resolve(eq(CMP), any())).thenReturn(complaintResp());
        mockMvc.perform(post("/api/v1/complaints/{id}/resolve", CMP).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void resolve_200_withCapa() throws Exception {
        when(service.resolve(eq(CMP), any())).thenReturn(complaintResp());
        mockMvc.perform(post("/api/v1/complaints/{id}/resolve", CMP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"capaCaseId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void close_200() throws Exception {
        when(service.close(CMP)).thenReturn(complaintResp());
        mockMvc.perform(post("/api/v1/complaints/{id}/close", CMP).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void reopen_200() throws Exception {
        when(service.reopen(CMP)).thenReturn(complaintResp());
        mockMvc.perform(post("/api/v1/complaints/{id}/reopen", CMP).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void satisfaction_outOfRange_400() throws Exception {
        mockMvc.perform(post("/api/v1/complaints/{id}/satisfaction", CMP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":15}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void satisfaction_200() throws Exception {
        when(service.setSatisfaction(eq(CMP), any())).thenReturn(complaintResp());
        mockMvc.perform(post("/api/v1/complaints/{id}/satisfaction", CMP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"score\":9}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void listResponses_200() throws Exception {
        when(service.listResponses(eq(CMP), any()))
                .thenReturn(new PageImpl<>(List.of(responseResp())));
        mockMvc.perform(get("/api/v1/complaints/{id}/responses", CMP))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void addResponse_201() throws Exception {
        when(service.addResponse(eq(CMP), any())).thenReturn(responseResp());
        ComplaintDto.AddResponseRequest req = new ComplaintDto.AddResponseRequest(
                USER, ComplaintChannel.EMAIL, "Hello", false);
        mockMvc.perform(post("/api/v1/complaints/{id}/responses", CMP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void addResponse_blankBody_400() throws Exception {
        String body = "{\"authorUserId\":\"" + USER + "\",\"body\":\"\"}";
        mockMvc.perform(post("/api/v1/complaints/{id}/responses", CMP).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void deleteResponse_204() throws Exception {
        mockMvc.perform(delete("/api/v1/complaints/{id}/responses/{rid}", CMP, RESP).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).deleteResponse(CMP, RESP);
    }

    @Test @WithMockUser
    void deleteResponse_externalImmutable_409() throws Exception {
        doThrow(new ComplaintStateException("immutable")).when(service).deleteResponse(CMP, RESP);
        mockMvc.perform(delete("/api/v1/complaints/{id}/responses/{rid}", CMP, RESP).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void deleteResponse_notFound_404() throws Exception {
        doThrow(new ComplaintResponseNotFoundException(RESP))
                .when(service).deleteResponse(CMP, RESP);
        mockMvc.perform(delete("/api/v1/complaints/{id}/responses/{rid}", CMP, RESP).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void statistics_200() throws Exception {
        when(service.statistics()).thenReturn(new ComplaintDto.ComplaintStatistics(
                TENANT, 42L, 3L, 5L, 8L, 12L, 10L, 4L,
                1L, 2L, 3L, 4L, 5L, 0L, 1L));
        mockMvc.perform(get("/api/v1/complaints/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(42));
    }

    // --- factories ---

    private ComplaintDto.ComplaintResponse complaintResp() {
        return new ComplaintDto.ComplaintResponse(
                CMP, TENANT, "C-1", ComplaintChannel.EMAIL,
                "Alice", "alice@example.test", null,
                "Slow", null, ComplaintSeverity.MEDIUM, ComplaintCategory.DELIVERY,
                ComplaintStatus.RECEIVED,
                null, null, null, null,
                Instant.now(), null, null, null, null,
                USER, Instant.now(), Instant.now());
    }

    private ComplaintDto.ResponseEntryResponse responseResp() {
        return new ComplaintDto.ResponseEntryResponse(
                RESP, TENANT, CMP, USER, ComplaintChannel.EMAIL,
                "Hello", false, Instant.now(), Instant.now());
    }
}
