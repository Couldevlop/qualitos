package com.openlab.qualitos.quality.docs;

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
@WebMvcTest(controllers = DocumentController.class)
class DocumentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DocumentService service;
    ObjectMapper om;

    static final UUID DOC = UUID.randomUUID();
    static final UUID VER = UUID.randomUUID();
    static final UUID TENANT = UUID.randomUUID();
    static final UUID OWNER = UUID.randomUUID();
    static final UUID USER = UUID.randomUUID();
    static final UUID APPROVER = UUID.randomUUID();

    @BeforeEach
    void setup() {
        om = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test @WithMockUser
    void list_returns200() throws Exception {
        when(service.findAll(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(docResp(DocumentStatus.ACTIVE))));
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(DOC.toString()));
    }

    @Test @WithMockUser
    void list_withFilter() throws Exception {
        when(service.findAll(eq(DocumentStatus.ARCHIVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(docResp(DocumentStatus.ARCHIVED))));
        mockMvc.perform(get("/api/v1/documents").param("status", "ARCHIVED"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void create_returns201() throws Exception {
        when(service.createDocument(any())).thenReturn(docResp(DocumentStatus.ACTIVE));
        DocumentDto.CreateDocumentRequest req = new DocumentDto.CreateDocumentRequest(
                "C", "T", null, DocumentType.PROCEDURE, OWNER, false, null, null, null);
        mockMvc.perform(post("/api/v1/documents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(DOC.toString()));
    }

    @Test @WithMockUser
    void create_missingCode_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/documents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"type\":\"POLICY\",\"ownerId\":\"" + OWNER + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void create_codeConflict_returns409() throws Exception {
        when(service.createDocument(any())).thenThrow(new DocumentCodeConflictException("X"));
        DocumentDto.CreateDocumentRequest req = new DocumentDto.CreateDocumentRequest(
                "X", "T", null, DocumentType.POLICY, OWNER, false, null, null, null);
        mockMvc.perform(post("/api/v1/documents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void get_notFound() throws Exception {
        when(service.findById(DOC)).thenThrow(new DocumentNotFoundException(DOC));
        mockMvc.perform(get("/api/v1/documents/{id}", DOC))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void get_success() throws Exception {
        when(service.findById(DOC)).thenReturn(docResp(DocumentStatus.ACTIVE));
        mockMvc.perform(get("/api/v1/documents/{id}", DOC)).andExpect(status().isOk());
    }

    @Test @WithMockUser
    void update_success() throws Exception {
        when(service.updateDocument(eq(DOC), any())).thenReturn(docResp(DocumentStatus.ACTIVE));
        mockMvc.perform(patch("/api/v1/documents/{id}", DOC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void archive_success() throws Exception {
        when(service.archiveDocument(DOC)).thenReturn(docResp(DocumentStatus.ARCHIVED));
        mockMvc.perform(patch("/api/v1/documents/{id}/archive", DOC).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void archive_alreadyArchived_returns409() throws Exception {
        when(service.archiveDocument(DOC)).thenThrow(new DocumentStateException("c"));
        mockMvc.perform(patch("/api/v1/documents/{id}/archive", DOC).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void createVersion_returns201() throws Exception {
        when(service.createVersion(eq(DOC), any())).thenReturn(verResp(VersionStatus.DRAFT));
        DocumentDto.CreateVersionRequest req = new DocumentDto.CreateVersionRequest(
                "content", null, "note", OWNER);
        mockMvc.perform(post("/api/v1/documents/{id}/versions", DOC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void createVersion_missingAuthor_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/documents/{id}/versions", DOC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void updateVersion_success() throws Exception {
        when(service.updateVersion(eq(DOC), eq(VER), any())).thenReturn(verResp(VersionStatus.DRAFT));
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void updateVersion_versionNotFound_returns404() throws Exception {
        when(service.updateVersion(eq(DOC), eq(VER), any()))
                .thenThrow(new DocumentVersionNotFoundException(VER));
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void submit_success() throws Exception {
        when(service.submitForReview(DOC, VER)).thenReturn(verResp(VersionStatus.IN_REVIEW));
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/submit", DOC, VER).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void submit_invalid_returns409() throws Exception {
        when(service.submitForReview(DOC, VER)).thenThrow(new DocumentStateException("nope"));
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/submit", DOC, VER).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void approve_success() throws Exception {
        when(service.approveVersion(eq(DOC), eq(VER), any())).thenReturn(verResp(VersionStatus.APPROVED));
        DocumentDto.ApprovalRequest req = new DocumentDto.ApprovalRequest(APPROVER);
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/approve", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void approve_missingApprover_returns400() throws Exception {
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/approve", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void publish_success() throws Exception {
        when(service.publishVersion(DOC, VER)).thenReturn(verResp(VersionStatus.PUBLISHED));
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/publish", DOC, VER).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void setBlockchain_success() throws Exception {
        when(service.setBlockchainTx(eq(DOC), eq(VER), eq("0xabc")))
                .thenReturn(verResp(VersionStatus.PUBLISHED));
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/blockchain", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"txHash\":\"0xabc\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser
    void acknowledge_returns201() throws Exception {
        when(service.acknowledge(eq(DOC), eq(VER), any())).thenReturn(
                new DocumentDto.AcknowledgmentResponse(UUID.randomUUID(), VER, USER, Instant.now()));
        DocumentDto.AcknowledgeRequest req = new DocumentDto.AcknowledgeRequest(USER);
        mockMvc.perform(post("/api/v1/documents/{id}/versions/{vid}/acknowledge", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void acknowledge_notMandatory_returns409() throws Exception {
        when(service.acknowledge(eq(DOC), eq(VER), any()))
                .thenThrow(new DocumentStateException("Document is not marked as mandatory-read"));
        DocumentDto.AcknowledgeRequest req = new DocumentDto.AcknowledgeRequest(USER);
        mockMvc.perform(post("/api/v1/documents/{id}/versions/{vid}/acknowledge", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void countAcks_returnsCount() throws Exception {
        when(service.countAcknowledgments(DOC, VER)).thenReturn(7L);
        mockMvc.perform(get("/api/v1/documents/{id}/versions/{vid}/acknowledgments/count", DOC, VER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(7));
    }

    @Test @WithMockUser
    void create_missingTenant_returns403() throws Exception {
        when(service.createDocument(any())).thenThrow(new MissingTenantContextException());
        DocumentDto.CreateDocumentRequest req = new DocumentDto.CreateDocumentRequest(
                "C", "T", null, DocumentType.POLICY, OWNER, false, null, null, null);
        mockMvc.perform(post("/api/v1/documents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    // helpers
    private DocumentDto.DocumentResponse docResp(DocumentStatus s) {
        return new DocumentDto.DocumentResponse(
                DOC, TENANT, "C", "T", null, DocumentType.PROCEDURE, s, OWNER, null,
                false, Instant.now(), Instant.now(), List.of());
    }

    private DocumentDto.VersionResponse verResp(VersionStatus s) {
        return new DocumentDto.VersionResponse(
                VER, DOC, 1, null, null, null, null, s, OWNER, null, null, null, null,
                Instant.now(), Instant.now());
    }
}
