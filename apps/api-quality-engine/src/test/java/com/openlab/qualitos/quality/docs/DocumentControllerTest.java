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
    @MockitoBean DocumentFileService files;
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

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void list_returns200() throws Exception {
        when(service.findAll(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(docResp(DocumentStatus.ACTIVE))));
        mockMvc.perform(get("/api/v1/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(DOC.toString()));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void list_withFilter() throws Exception {
        when(service.findAll(eq(DocumentStatus.ARCHIVED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(docResp(DocumentStatus.ARCHIVED))));
        mockMvc.perform(get("/api/v1/documents").param("status", "ARCHIVED"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void list_withGedFilters_searches() throws Exception {
        UUID folder = UUID.randomUUID();
        when(service.search(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(docResp(DocumentStatus.ACTIVE))));
        mockMvc.perform(get("/api/v1/documents")
                        .param("view", "MINE").param("folderId", folder.toString()).param("q", "achat")
                        .param("displayStatus", "IN_FORCE").param("type", "PROCEDURE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].displayStatus").value("DRAFT"));
        verify(service).search(eq(new DocumentDto.ListFilter(DocumentView.MINE, folder, "achat",
                DisplayStatus.IN_FORCE, DocumentType.PROCEDURE, null, null)), any(Pageable.class));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void list_statusWithNewFilter_mapsStatusToView() throws Exception {
        when(service.search(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));
        mockMvc.perform(get("/api/v1/documents").param("status", "ARCHIVED").param("q", "x"))
                .andExpect(status().isOk());
        verify(service).search(eq(new DocumentDto.ListFilter(DocumentView.ARCHIVED, null, "x",
                null, null, null, null)), any(Pageable.class));
    }

    @Test @WithMockUser(roles = "USER")
    void views_returnsCounters() throws Exception {
        when(service.viewCounts()).thenReturn(new DocumentDto.ViewCounts(10, 2, 1, 3, 4));
        mockMvc.perform(get("/api/v1/documents/views"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toValidate").value(1))
                .andExpect(jsonPath("$.reviewDue").value(3));
    }

    @Test @WithMockUser(roles = "USER")
    void fileLink_returnsPresignedLink() throws Exception {
        when(files.link(DOC, VER, true)).thenReturn(new DocumentDto.FileLink(
                "https://storage.test/x", "proc.pdf", "application/pdf", 12, 600));
        mockMvc.perform(get("/api/v1/documents/" + DOC + "/versions/" + VER + "/file").param("download", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("https://storage.test/x"))
                .andExpect(jsonPath("$.fileName").value("proc.pdf"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
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

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void create_missingCode_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/documents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"T\",\"type\":\"POLICY\",\"ownerId\":\"" + OWNER + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void create_codeConflict_returns409() throws Exception {
        when(service.createDocument(any())).thenThrow(new DocumentCodeConflictException("X"));
        DocumentDto.CreateDocumentRequest req = new DocumentDto.CreateDocumentRequest(
                "X", "T", null, DocumentType.POLICY, OWNER, false, null, null, null);
        mockMvc.perform(post("/api/v1/documents").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void get_notFound() throws Exception {
        when(service.findById(DOC)).thenThrow(new DocumentNotFoundException(DOC));
        mockMvc.perform(get("/api/v1/documents/{id}", DOC))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void get_success() throws Exception {
        when(service.findById(DOC)).thenReturn(docResp(DocumentStatus.ACTIVE));
        mockMvc.perform(get("/api/v1/documents/{id}", DOC)).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void update_success() throws Exception {
        when(service.updateDocument(eq(DOC), any())).thenReturn(docResp(DocumentStatus.ACTIVE));
        mockMvc.perform(patch("/api/v1/documents/{id}", DOC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void archive_success() throws Exception {
        when(service.archiveDocument(DOC)).thenReturn(docResp(DocumentStatus.ARCHIVED));
        mockMvc.perform(patch("/api/v1/documents/{id}/archive", DOC).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void archive_alreadyArchived_returns409() throws Exception {
        when(service.archiveDocument(DOC)).thenThrow(new DocumentStateException("c"));
        mockMvc.perform(patch("/api/v1/documents/{id}/archive", DOC).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void createVersion_returns201() throws Exception {
        when(service.createVersion(eq(DOC), any())).thenReturn(verResp(VersionStatus.DRAFT));
        DocumentDto.CreateVersionRequest req = new DocumentDto.CreateVersionRequest(
                "content", null, "note", OWNER);
        mockMvc.perform(post("/api/v1/documents/{id}/versions", DOC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void createVersion_withoutAuthor_isAccepted_authorComesFromTheToken() throws Exception {
        // ADR 0080 : l'auteur n'est plus lu dans le corps ; le service le prend du jeton.
        when(service.createVersion(eq(DOC), any())).thenReturn(verResp(VersionStatus.DRAFT));
        mockMvc.perform(post("/api/v1/documents/{id}/versions", DOC).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void updateVersion_success() throws Exception {
        when(service.updateVersion(eq(DOC), eq(VER), any())).thenReturn(verResp(VersionStatus.DRAFT));
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void updateVersion_versionNotFound_returns404() throws Exception {
        when(service.updateVersion(eq(DOC), eq(VER), any()))
                .thenThrow(new DocumentVersionNotFoundException(VER));
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void submit_success() throws Exception {
        when(service.submitForReview(DOC, VER)).thenReturn(verResp(VersionStatus.IN_REVIEW));
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/submit", DOC, VER).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void submit_invalid_returns409() throws Exception {
        when(service.submitForReview(DOC, VER)).thenThrow(new DocumentStateException("nope"));
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/submit", DOC, VER).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void approve_success() throws Exception {
        when(service.approveVersion(eq(DOC), eq(VER), any())).thenReturn(verResp(VersionStatus.APPROVED));
        DocumentDto.ApprovalRequest req = new DocumentDto.ApprovalRequest(APPROVER);
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/approve", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void approve_withoutApprover_isAccepted_approverComesFromTheToken() throws Exception {
        // ADR 0080 : l'approbateur est l'utilisateur du jeton.
        when(service.approveVersion(eq(DOC), eq(VER), any())).thenReturn(verResp(VersionStatus.APPROVED));
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/approve", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void reject_withReason_ok() throws Exception {
        when(service.rejectVersion(eq(DOC), eq(VER), any())).thenReturn(verResp(VersionStatus.DRAFT));
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/reject", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Section 4 incomplète\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void reject_withoutReason_400() throws Exception {
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/reject", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"  \"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).rejectVersion(any(), any(), any());
    }

    @Test @WithMockUser(roles = "USER")
    void reject_withoutTheRightToApprove_403() throws Exception {
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/reject", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"non\"}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void publish_success() throws Exception {
        when(service.publishVersion(DOC, VER)).thenReturn(verResp(VersionStatus.PUBLISHED));
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/publish", DOC, VER).with(csrf()))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void setBlockchain_success() throws Exception {
        when(service.setBlockchainTx(eq(DOC), eq(VER), eq("0xabc")))
                .thenReturn(verResp(VersionStatus.PUBLISHED));
        mockMvc.perform(patch("/api/v1/documents/{id}/versions/{vid}/blockchain", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"txHash\":\"0xabc\"}"))
                .andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void acknowledge_returns201() throws Exception {
        when(service.acknowledge(eq(DOC), eq(VER), any())).thenReturn(
                new DocumentDto.AcknowledgmentResponse(UUID.randomUUID(), VER, USER, Instant.now()));
        DocumentDto.AcknowledgeRequest req = new DocumentDto.AcknowledgeRequest(USER);
        mockMvc.perform(post("/api/v1/documents/{id}/versions/{vid}/acknowledge", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void acknowledge_notMandatory_returns409() throws Exception {
        when(service.acknowledge(eq(DOC), eq(VER), any()))
                .thenThrow(new DocumentStateException("Document is not marked as mandatory-read"));
        DocumentDto.AcknowledgeRequest req = new DocumentDto.AcknowledgeRequest(USER);
        mockMvc.perform(post("/api/v1/documents/{id}/versions/{vid}/acknowledge", DOC, VER).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void countAcks_returnsCount() throws Exception {
        when(service.countAcknowledgments(DOC, VER)).thenReturn(7L);
        mockMvc.perform(get("/api/v1/documents/{id}/versions/{vid}/acknowledgments/count", DOC, VER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(7));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
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
                false, Instant.now(), Instant.now(), List.of(),
                null, null, DocumentLifecycle.DRAFT, DisplayStatus.DRAFT, null);
    }

    private DocumentDto.VersionResponse verResp(VersionStatus s) {
        return new DocumentDto.VersionResponse(
                VER, DOC, 1, null, null, null, null, s, OWNER, null, null, null, null,
                Instant.now(), Instant.now(), null, null, null, null);
    }
}
