package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Façade HTTP des documents d'audit : la traduction de chaque refus en code distinct. */
@Tag("web")
@WebMvcTest(controllers = AuditDocumentController.class)
class AuditDocumentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuditDocumentService service;

    static final UUID PLAN = UUID.randomUUID();
    static final UUID DOC = UUID.randomUUID();
    static final UUID ME = UUID.randomUUID();

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "convocation.pdf", "application/pdf", "%PDF-1.7 x".getBytes());
    }

    @Test
    void depot_201_auteurLuDansLeJeton() throws Exception {
        when(service.upload(eq(PLAN), eq("application/pdf"), eq("convocation.pdf"), any(), eq(ME)))
                .thenReturn(new AuditDocumentDto.Response(DOC, PLAN, "application/pdf", 10L,
                        "convocation.pdf", ME, Instant.parse("2026-10-14T10:00:00Z")));

        mockMvc.perform(multipart("/api/v1/audits/plans/{id}/documents", PLAN).file(pdf())
                        .with(jwt().jwt(j -> j.subject(ME.toString()))).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(DOC.toString()))
                .andExpect(jsonPath("$.uploadedBy").value(ME.toString()));
    }

    @Test @WithMockUser
    void depot_fichierVide_400() throws Exception {
        MockMultipartFile vide = new MockMultipartFile("file", "v.pdf", "application/pdf", new byte[0]);
        mockMvc.perform(multipart("/api/v1/audits/plans/{id}/documents", PLAN).file(vide).with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser
    void depot_typeRefuse_400() throws Exception {
        when(service.upload(any(), any(), any(), any(), any()))
                .thenThrow(new AuditDocumentValidationException("Unsupported content type"));
        mockMvc.perform(multipart("/api/v1/audits/plans/{id}/documents", PLAN).file(pdf()).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/audit-document-invalid"));
    }

    @Test @WithMockUser
    void depot_tropLourd_413() throws Exception {
        when(service.upload(any(), any(), any(), any(), any()))
                .thenThrow(new AuditDocumentTooLargeException("too large"));
        mockMvc.perform(multipart("/api/v1/audits/plans/{id}/documents", PLAN).file(pdf()).with(csrf()))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test @WithMockUser
    void depot_rapportEmis_409() throws Exception {
        when(service.upload(any(), any(), any(), any(), any()))
                .thenThrow(new AuditStateException("locked"));
        mockMvc.perform(multipart("/api/v1/audits/plans/{id}/documents", PLAN).file(pdf()).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void liste_200() throws Exception {
        when(service.list(PLAN)).thenReturn(List.of(new AuditDocumentDto.ListItem(DOC, PLAN, "application/pdf",
                10L, "convocation.pdf", null, Instant.parse("2026-10-14T10:00:00Z"), "https://minio/x")));
        mockMvc.perform(get("/api/v1/audits/plans/{id}/documents", PLAN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].url").value("https://minio/x"));
    }

    @Test @WithMockUser
    void liste_stockageCoupe_503() throws Exception {
        when(service.list(PLAN)).thenThrow(new StorageDisabledException());
        mockMvc.perform(get("/api/v1/audits/plans/{id}/documents", PLAN))
                .andExpect(status().isServiceUnavailable());
    }

    @Test @WithMockUser
    void retrait_204() throws Exception {
        mockMvc.perform(delete("/api/v1/audits/plans/{id}/documents/{doc}", PLAN, DOC).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).delete(eq(PLAN), eq(DOC), any());
    }

    @Test @WithMockUser
    void retrait_introuvable_404() throws Exception {
        doThrow(new AuditDocumentNotFoundException(DOC)).when(service).delete(eq(PLAN), eq(DOC), any());
        mockMvc.perform(delete("/api/v1/audits/plans/{id}/documents/{doc}", PLAN, DOC).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
