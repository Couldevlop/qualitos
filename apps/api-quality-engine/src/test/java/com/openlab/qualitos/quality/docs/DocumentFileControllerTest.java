package com.openlab.qualitos.quality.docs;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = DocumentFileController.class)
class DocumentFileControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DocumentFileService files;

    static final UUID DOC = UUID.randomUUID();
    static final UUID VER = UUID.randomUUID();
    static final byte[] PDF = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
    static final String URL = "/api/v1/document-files/" + DOC + "/versions/" + VER;

    static DocumentDto.VersionResponse resp() {
        return new DocumentDto.VersionResponse(VER, DOC, 1, null, null, "abc", null, VersionStatus.DRAFT,
                UUID.randomUUID(), null, null, null, null, Instant.now(), Instant.now(), null, null, null,
                new DocumentDto.FileInfo("proc.pdf", "application/pdf", PDF.length, true, false));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void upload_200() throws Exception {
        when(files.upload(eq(DOC), eq(VER), eq("application/pdf"), eq("proc.pdf"), any())).thenReturn(resp());
        mockMvc.perform(multipart(URL).file(new MockMultipartFile("file", "proc.pdf", "application/pdf", PDF))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.file.fileName").value("proc.pdf"))
                .andExpect(jsonPath("$.contentHash").value("abc"));
        verify(files).upload(DOC, VER, "application/pdf", "proc.pdf", PDF);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void upload_fichierVide_400() throws Exception {
        mockMvc.perform(multipart(URL).file(new MockMultipartFile("file", "vide.pdf", "application/pdf", new byte[0]))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(files);
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void upload_refuse_400() throws Exception {
        when(files.upload(any(), any(), any(), any(), any()))
                .thenThrow(new DocumentFileRejectedException("File content does not match its type"));
        mockMvc.perform(multipart(URL).file(new MockMultipartFile("file", "x.pdf", "application/pdf", PDF))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("File content does not match its type"));
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void upload_tropGros_413() throws Exception {
        when(files.upload(any(), any(), any(), any(), any()))
                .thenThrow(new DocumentFileTooLargeException("quota"));
        mockMvc.perform(multipart(URL).file(new MockMultipartFile("file", "x.pdf", "application/pdf", PDF))
                        .with(csrf()))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void upload_versionHorsBrouillon_409() throws Exception {
        when(files.upload(any(), any(), any(), any(), any()))
                .thenThrow(new DocumentStateException("A file can only be attached to a DRAFT version"));
        mockMvc.perform(multipart(URL).file(new MockMultipartFile("file", "x.pdf", "application/pdf", PDF))
                        .with(csrf()))
                .andExpect(status().isConflict());
    }
}
