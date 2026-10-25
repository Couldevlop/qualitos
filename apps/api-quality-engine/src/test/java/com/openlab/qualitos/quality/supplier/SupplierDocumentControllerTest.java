package com.openlab.qualitos.quality.supplier;

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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Façade HTTP des documents fournisseur : chaque refus traduit en code distinct. */
@Tag("web")
@WebMvcTest(controllers = SupplierDocumentController.class)
class SupplierDocumentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SupplierDocumentService service;

    static final UUID SUP = UUID.randomUUID();
    static final UUID NC = UUID.randomUUID();
    static final UUID CERT = UUID.randomUUID();
    static final UUID DOC = UUID.randomUUID();
    static final UUID ME = UUID.randomUUID();

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "8D.pdf", "application/pdf", "%PDF-1.7 x".getBytes());
    }

    private SupplierDocumentDto.Response response(SupplierDocumentOwnerType type, UUID owner,
                                                  SupplierDocumentCategory category) {
        return new SupplierDocumentDto.Response(DOC, SUP, type, owner, category, "application/pdf", 10L,
                "8D.pdf", ME, Instant.parse("2026-10-24T10:00:00Z"));
    }

    @Test
    void depotSurNc_201_natureEtAuteurTransmis() throws Exception {
        when(service.upload(eq(SUP), eq(SupplierDocumentOwnerType.NON_CONFORMITY), eq(NC),
                eq(SupplierDocumentCategory.EIGHT_D), eq("application/pdf"), eq("8D.pdf"), any(), eq(ME)))
                .thenReturn(response(SupplierDocumentOwnerType.NON_CONFORMITY, NC, SupplierDocumentCategory.EIGHT_D));

        mockMvc.perform(multipart("/api/v1/suppliers/{id}/non-conformities/{ncId}/documents", SUP, NC)
                        .file(pdf()).param("category", "EIGHT_D")
                        .with(jwt().jwt(j -> j.subject(ME.toString()))).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("EIGHT_D"))
                .andExpect(jsonPath("$.uploadedBy").value(ME.toString()));
    }

    @Test @WithMockUser
    void depotSurCertificat_201_sansNature() throws Exception {
        when(service.upload(eq(SUP), eq(SupplierDocumentOwnerType.CERTIFICATE), eq(CERT), isNull(),
                any(), any(), any(), any()))
                .thenReturn(response(SupplierDocumentOwnerType.CERTIFICATE, CERT, SupplierDocumentCategory.CERTIFICATE));

        mockMvc.perform(multipart("/api/v1/suppliers/{id}/certificates/{certId}/documents", SUP, CERT)
                        .file(pdf()).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("CERTIFICATE"));
    }

    @Test @WithMockUser
    void depot_fichierVide_400() throws Exception {
        MockMultipartFile vide = new MockMultipartFile("file", "v.pdf", "application/pdf", new byte[0]);
        mockMvc.perform(multipart("/api/v1/suppliers/{id}/non-conformities/{ncId}/documents", SUP, NC)
                        .file(vide).with(csrf()))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test @WithMockUser
    void depot_refuse_400_413_409() throws Exception {
        when(service.upload(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new SupplierDocumentValidationException("bad"))
                .thenThrow(new SupplierDocumentTooLargeException("big"))
                .thenThrow(new SupplierStateException("closed"));
        String url = "/api/v1/suppliers/{id}/non-conformities/{ncId}/documents";
        mockMvc.perform(multipart(url, SUP, NC).file(pdf()).with(csrf())).andExpect(status().isBadRequest());
        mockMvc.perform(multipart(url, SUP, NC).file(pdf()).with(csrf())).andExpect(status().isPayloadTooLarge());
        mockMvc.perform(multipart(url, SUP, NC).file(pdf()).with(csrf())).andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void liste_200_etStockageCoupe_503() throws Exception {
        when(service.list(SUP, SupplierDocumentOwnerType.NON_CONFORMITY, NC)).thenReturn(List.of(
                new SupplierDocumentDto.ListItem(DOC, SUP, SupplierDocumentOwnerType.NON_CONFORMITY, NC,
                        SupplierDocumentCategory.CAPA_EVIDENCE, "application/pdf", 10L, "preuve.pdf", ME,
                        Instant.parse("2026-10-24T10:00:00Z"), "https://minio/x?sig=1")));
        mockMvc.perform(get("/api/v1/suppliers/{id}/non-conformities/{ncId}/documents", SUP, NC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].url").value("https://minio/x?sig=1"));

        when(service.list(SUP, SupplierDocumentOwnerType.CERTIFICATE, CERT)).thenThrow(new StorageDisabledException());
        mockMvc.perform(get("/api/v1/suppliers/{id}/certificates/{certId}/documents", SUP, CERT))
                .andExpect(status().isServiceUnavailable());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void retrait_204_surNcEtCertificat() throws Exception {
        mockMvc.perform(delete("/api/v1/suppliers/{id}/non-conformities/{ncId}/documents/{d}", SUP, NC, DOC)
                        .with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/v1/suppliers/{id}/certificates/{certId}/documents/{d}", SUP, CERT, DOC)
                        .with(csrf()))
                .andExpect(status().isNoContent());
        verify(service).delete(eq(SUP), eq(SupplierDocumentOwnerType.NON_CONFORMITY), eq(NC), eq(DOC), any());
        verify(service).delete(eq(SUP), eq(SupplierDocumentOwnerType.CERTIFICATE), eq(CERT), eq(DOC), any());
    }

    @Test @WithMockUser
    void vueGed_transmetLesFiltres() throws Exception {
        when(service.listAll(eq(SUP), eq(SupplierDocumentOwnerType.CERTIFICATE), eq(SupplierDocumentCategory.CERTIFICATE),
                eq("iso"), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(
                new SupplierDocumentDto.CatalogItem(DOC, SUP, "ACME", "Acme", SupplierDocumentOwnerType.CERTIFICATE,
                        CERT, "iso-9001", SupplierDocumentCategory.CERTIFICATE, "application/pdf", 10L, "iso.pdf",
                        ME, Instant.parse("2026-10-24T10:00:00Z"), "https://minio/x"))));
        mockMvc.perform(get("/api/v1/suppliers/documents")
                        .param("supplierId", SUP.toString()).param("ownerType", "CERTIFICATE")
                        .param("category", "CERTIFICATE").param("q", "iso"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ownerLabel").value("iso-9001"));
    }

    @Test @WithMockUser
    void retrait_documentInconnu_404() throws Exception {
        doThrow(new SupplierChildNotFoundException("Document", DOC))
                .when(service).delete(any(), any(), any(), any(), any());
        mockMvc.perform(delete("/api/v1/suppliers/{id}/non-conformities/{ncId}/documents/{d}", SUP, NC, DOC)
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
