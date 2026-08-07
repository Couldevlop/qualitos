package com.openlab.qualitos.quality.capa;

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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Façade HTTP des preuves d'un dossier CAPA (§4.2).
 *
 * <p>Ce qui se teste ici, c'est la traduction des refus. Un dossier de preuve
 * refuse pour six raisons distinctes — type inadmissible, signature incohérente,
 * pièce trop lourde, borne atteinte, dossier clos, stockage coupé — et chacune
 * appelle une conduite différente côté client. Les confondre sous un même code
 * reviendrait à dire « non » sans dire pourquoi.
 */
@Tag("web")
@WebMvcTest(controllers = CapaEvidenceController.class)
class CapaEvidenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CapaEvidenceService service;

    private static final UUID CAPA = UUID.randomUUID();
    private static final UUID EVIDENCE = UUID.randomUUID();

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "releve.pdf", "application/pdf", "%PDF-1.7 x".getBytes());
    }

    // --- dépôt ------------------------------------------------------------------

    @Test
    @WithMockUser
    void depot_retourne201() throws Exception {
        when(service.upload(eq(CAPA), eq("application/pdf"), eq("releve.pdf"), any(), any()))
                .thenReturn(new CapaEvidenceDto.Response(EVIDENCE, CAPA, "application/pdf",
                        10L, "releve.pdf", null, Instant.parse("2026-08-07T10:00:00Z")));

        mockMvc.perform(multipart("/api/v1/capa/cases/{id}/evidences", CAPA).file(pdf()).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(EVIDENCE.toString()))
                .andExpect(jsonPath("$.originalFilename").value("releve.pdf"));
    }

    @Test
    @WithMockUser
    void depot_sansFichier_retourne400() throws Exception {
        MockMultipartFile vide = new MockMultipartFile("file", "vide.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart("/api/v1/capa/cases/{id}/evidences", CAPA).file(vide).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void depot_dUnTypeInadmissible_retourne400() throws Exception {
        when(service.upload(any(), any(), any(), any(), any()))
                .thenThrow(new CapaEvidenceValidationException("Unsupported content type: application/x-msdownload"));

        mockMvc.perform(multipart("/api/v1/capa/cases/{id}/evidences", CAPA).file(pdf()).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid CAPA Evidence"));
    }

    @Test
    @WithMockUser
    void depot_dUnePieceTropLourde_retourne413() throws Exception {
        when(service.upload(any(), any(), any(), any(), any()))
                .thenThrow(new CapaEvidenceTooLargeException(20_000_000L, 10_485_760L));

        mockMvc.perform(multipart("/api/v1/capa/cases/{id}/evidences", CAPA).file(pdf()).with(csrf()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.title").value("CAPA Evidence Too Large"));
    }

    @Test
    @WithMockUser
    void depot_surUneBorneAtteinte_retourne409() throws Exception {
        // Borne atteinte : un conflit d'état, pas une panne ni une entrée invalide.
        when(service.upload(any(), any(), any(), any(), any()))
                .thenThrow(new CapaStateException("This CAPA already carries its 10 evidence files"));

        mockMvc.perform(multipart("/api/v1/capa/cases/{id}/evidences", CAPA).file(pdf()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("10 evidence files")));
    }

    @Test
    @WithMockUser
    void depot_surUnDossierInconnu_retourne404() throws Exception {
        when(service.upload(any(), any(), any(), any(), any()))
                .thenThrow(new CapaNotFoundException(CAPA));

        mockMvc.perform(multipart("/api/v1/capa/cases/{id}/evidences", CAPA).file(pdf()).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void depot_stockageCoupe_retourne503() throws Exception {
        when(service.upload(any(), any(), any(), any(), any()))
                .thenThrow(new StorageDisabledException());

        mockMvc.perform(multipart("/api/v1/capa/cases/{id}/evidences", CAPA).file(pdf()).with(csrf()))
                .andExpect(status().isServiceUnavailable());
    }

    // --- lecture ------------------------------------------------------------------

    @Test
    @WithMockUser
    void liste_retourne200_avecLUrlDeLecture() throws Exception {
        when(service.list(CAPA)).thenReturn(List.of(new CapaEvidenceDto.ListItem(
                EVIDENCE, CAPA, "application/pdf", 2048L, "releve.pdf", null,
                Instant.parse("2026-08-07T10:00:00Z"), "https://stockage.example/x?sig=abc")));

        mockMvc.perform(get("/api/v1/capa/cases/{id}/evidences", CAPA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sizeBytes").value(2048))
                .andExpect(jsonPath("$[0].url").value("https://stockage.example/x?sig=abc"));
    }

    @Test
    @WithMockUser
    void liste_stockageCoupe_retourne503_plutotQueUnDossierVide() throws Exception {
        // Rendre une liste vide laisserait croire à un dossier sans preuve : c'est
        // exactement l'inverse de ce qu'un auditeur doit comprendre.
        when(service.list(CAPA)).thenThrow(new StorageDisabledException());

        mockMvc.perform(get("/api/v1/capa/cases/{id}/evidences", CAPA))
                .andExpect(status().isServiceUnavailable());
    }

    // --- retrait --------------------------------------------------------------------

    @Test
    @WithMockUser
    void retrait_retourne204() throws Exception {
        doNothing().when(service).delete(CAPA, EVIDENCE);

        mockMvc.perform(delete("/api/v1/capa/cases/{id}/evidences/{evidenceId}", CAPA, EVIDENCE)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).delete(CAPA, EVIDENCE);
    }

    @Test
    @WithMockUser
    void retrait_dUnePieceInconnue_retourne404() throws Exception {
        doThrow(new CapaEvidenceNotFoundException(EVIDENCE)).when(service).delete(CAPA, EVIDENCE);

        mockMvc.perform(delete("/api/v1/capa/cases/{id}/evidences/{evidenceId}", CAPA, EVIDENCE)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("CAPA Evidence Not Found"));
    }

    @Test
    @WithMockUser
    void retrait_surUnDossierClos_retourne409() throws Exception {
        doThrow(new CapaStateException("Cannot change evidence on a CLOSED CAPA"))
                .when(service).delete(CAPA, EVIDENCE);

        mockMvc.perform(delete("/api/v1/capa/cases/{id}/evidences/{evidenceId}", CAPA, EVIDENCE)
                        .with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void refuse_unIdentifiantMalforme() throws Exception {
        mockMvc.perform(get("/api/v1/capa/cases/{id}/evidences", "pas-un-uuid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
