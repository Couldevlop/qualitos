package com.openlab.qualitos.quality.nonconformity;

import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("web")
@WebMvcTest(controllers = NcPhotoController.class)
class NcPhotoControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean NcPhotoService service;

    static final UUID NC = UUID.randomUUID();
    static final UUID PHOTO = UUID.randomUUID();

    private MockMultipartFile file(String contentType) {
        return new MockMultipartFile("file", "photo.png", contentType, new byte[]{1, 2, 3});
    }

    // --- 503 when storage OFF (default behaviour) ---
    @Test @WithMockUser
    void upload_storageDisabled_returns503() throws Exception {
        when(service.upload(eq(NC), any(), any(), any())).thenThrow(new StorageDisabledException());
        mockMvc.perform(multipart("/api/v1/nc/{id}/photos", NC).file(file("image/png")).with(csrf()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/storage-disabled"));
    }

    @Test @WithMockUser
    void list_storageDisabled_returns503() throws Exception {
        when(service.list(NC)).thenThrow(new StorageDisabledException());
        mockMvc.perform(get("/api/v1/nc/{id}/photos", NC))
                .andExpect(status().isServiceUnavailable());
    }

    // --- happy paths (storage ON) ---
    @Test @WithMockUser
    void upload_returns201() throws Exception {
        when(service.upload(eq(NC), eq("image/png"), any(), any()))
                .thenReturn(new NcPhotoDto.Response(PHOTO, NC, "tenants/t/nc/n/x.png",
                        "image/png", 3L, "photo.png", Instant.now()));
        mockMvc.perform(multipart("/api/v1/nc/{id}/photos", NC).file(file("image/png")).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PHOTO.toString()))
                .andExpect(jsonPath("$.objectKey").value("tenants/t/nc/n/x.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.sizeBytes").value(3));
    }

    @Test @WithMockUser
    void upload_missingFilePart_returns400() throws Exception {
        // Pas de part 'file' → MissingServletRequestPartException → 400.
        mockMvc.perform(multipart("/api/v1/nc/{id}/photos", NC).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser
    void upload_emptyFile_returns400() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", "e.png", "image/png", new byte[0]);
        mockMvc.perform(multipart("/api/v1/nc/{id}/photos", NC).file(empty).with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser
    void upload_unsupportedType_returns400() throws Exception {
        when(service.upload(eq(NC), eq("application/pdf"), any(), any()))
                .thenThrow(new NcPhotoValidationException("Unsupported content type"));
        mockMvc.perform(multipart("/api/v1/nc/{id}/photos", NC).file(file("application/pdf")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://qualitos.io/errors/non-conformity-photo-invalid"));
    }

    @Test @WithMockUser
    void upload_tooLarge_returns413() throws Exception {
        when(service.upload(eq(NC), any(), any(), any()))
                .thenThrow(new NcPhotoTooLargeException(99L, 10L));
        mockMvc.perform(multipart("/api/v1/nc/{id}/photos", NC).file(file("image/png")).with(csrf()))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test @WithMockUser
    void upload_closedNc_returns409() throws Exception {
        when(service.upload(eq(NC), any(), any(), any()))
                .thenThrow(new NcStateException("Cannot attach a photo to a CLOSED non-conformity"));
        mockMvc.perform(multipart("/api/v1/nc/{id}/photos", NC).file(file("image/png")).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void upload_ncNotFound_returns404() throws Exception {
        when(service.upload(eq(NC), any(), any(), any())).thenThrow(new NcNotFoundException(NC));
        mockMvc.perform(multipart("/api/v1/nc/{id}/photos", NC).file(file("image/png")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test @WithMockUser
    void list_returns200_withUrls() throws Exception {
        when(service.list(NC)).thenReturn(List.of(new NcPhotoDto.ListItem(
                PHOTO, NC, "tenants/t/nc/n/x.png", "image/png", 3L, "photo.png",
                Instant.now(), "https://storage.test/x?ttl=900")));
        mockMvc.perform(get("/api/v1/nc/{id}/photos", NC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PHOTO.toString()))
                .andExpect(jsonPath("$[0].url").value("https://storage.test/x?ttl=900"));
    }

    @Test @WithMockUser
    void delete_returns204() throws Exception {
        doNothing().when(service).delete(NC, PHOTO);
        mockMvc.perform(delete("/api/v1/nc/{id}/photos/{photoId}", NC, PHOTO).with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).delete(NC, PHOTO);
    }

    @Test @WithMockUser
    void delete_notFound_returns404() throws Exception {
        doThrow(new NcPhotoNotFoundException(PHOTO)).when(service).delete(NC, PHOTO);
        mockMvc.perform(delete("/api/v1/nc/{id}/photos/{photoId}", NC, PHOTO).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
