package com.openlab.qualitos.quality.capa;

import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Façade HTTP des preuves d'ACTION (§4.2, ADR 0052).
 *
 * <p>Ce qui se teste ici, c'est que le second niveau de rattachement n'invente
 * aucun code de refus : ce sont exactement ceux du dossier — 400, 404, 409, 413,
 * 503 — plus le 404 propre à l'action introuvable. Un client qui sait traiter
 * les preuves de dossier ne doit rien avoir à apprendre.
 */
@Tag("web")
@WebMvcTest(controllers = CapaActionEvidenceController.class)
class CapaActionEvidenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CapaEvidenceService service;

    private static final UUID CAPA = UUID.randomUUID();
    private static final UUID ACTION = UUID.randomUUID();
    private static final UUID EVIDENCE = UUID.randomUUID();

    private static final String UPLOAD = "/api/v1/capa/cases/{id}/actions/{actionId}/evidences";

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "constat.pdf", "application/pdf", "%PDF-1.7 x".getBytes());
    }

    // --- dépôt ---------------------------------------------------------------

    @Test
    @WithMockUser
    void depot_retourne201_etPorteLActionVisee() throws Exception {
        when(service.uploadForAction(eq(CAPA), eq(ACTION), eq("application/pdf"),
                eq("constat.pdf"), any(), any()))
                .thenReturn(new CapaEvidenceDto.Response(EVIDENCE, CAPA, ACTION, "application/pdf",
                        10L, "constat.pdf", null, Instant.parse("2026-08-09T10:00:00Z")));

        mockMvc.perform(multipart(UPLOAD, CAPA, ACTION).file(pdf()).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(EVIDENCE.toString()))
                .andExpect(jsonPath("$.actionId").value(ACTION.toString()));
    }

    @Test
    @WithMockUser
    void depot_sansFichier_retourne400() throws Exception {
        MockMultipartFile vide = new MockMultipartFile("file", "vide.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart(UPLOAD, CAPA, ACTION).file(vide).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void depot_dUnTypeInadmissible_retourne400() throws Exception {
        when(service.uploadForAction(any(), any(), any(), any(), any(), any()))
                .thenThrow(new CapaEvidenceValidationException("Unsupported content type"));

        mockMvc.perform(multipart(UPLOAD, CAPA, ACTION).file(pdf()).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid CAPA Evidence"));
    }

    @Test
    @WithMockUser
    void depot_surUneActionInconnue_retourne404() throws Exception {
        when(service.uploadForAction(any(), any(), any(), any(), any(), any()))
                .thenThrow(new CapaActionNotFoundException(ACTION));

        mockMvc.perform(multipart(UPLOAD, CAPA, ACTION).file(pdf()).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("CAPA Action Not Found"));
    }

    @Test
    @WithMockUser
    void depot_dUneSecondePiece_retourne409() throws Exception {
        when(service.uploadForAction(any(), any(), any(), any(), any(), any()))
                .thenThrow(new CapaStateException("This action already carries its evidence file"));

        mockMvc.perform(multipart(UPLOAD, CAPA, ACTION).file(pdf()).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        org.hamcrest.Matchers.containsString("already carries")));
    }

    @Test
    @WithMockUser
    void depot_dUnePieceTropLourde_retourne413() throws Exception {
        when(service.uploadForAction(any(), any(), any(), any(), any(), any()))
                .thenThrow(new CapaEvidenceTooLargeException(20_000_000L, 10_485_760L));

        mockMvc.perform(multipart(UPLOAD, CAPA, ACTION).file(pdf()).with(csrf()))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test
    @WithMockUser
    void depot_stockageCoupe_retourne503() throws Exception {
        when(service.uploadForAction(any(), any(), any(), any(), any(), any()))
                .thenThrow(new StorageDisabledException());

        mockMvc.perform(multipart(UPLOAD, CAPA, ACTION).file(pdf()).with(csrf()))
                .andExpect(status().isServiceUnavailable());
    }

    // --- lecture -------------------------------------------------------------

    @Test
    @WithMockUser
    void liste_retourne200_avecLUrlEtLAction() throws Exception {
        when(service.listForActions(CAPA)).thenReturn(List.of(new CapaEvidenceDto.ListItem(
                EVIDENCE, CAPA, ACTION, "application/pdf", 2048L, "constat.pdf", null,
                Instant.parse("2026-08-09T10:00:00Z"), "https://stockage.example/x?sig=abc")));

        mockMvc.perform(get("/api/v1/capa/cases/{id}/action-evidences", CAPA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].actionId").value(ACTION.toString()))
                .andExpect(jsonPath("$[0].url").value("https://stockage.example/x?sig=abc"));
    }

    @Test
    @WithMockUser
    void liste_stockageCoupe_retourne503_plutotQuUnTableauSansPreuve() throws Exception {
        when(service.listForActions(CAPA)).thenThrow(new StorageDisabledException());

        mockMvc.perform(get("/api/v1/capa/cases/{id}/action-evidences", CAPA))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @WithMockUser
    void refuse_unIdentifiantDeDossierMalforme() throws Exception {
        mockMvc.perform(get("/api/v1/capa/cases/{id}/action-evidences", "pas-un-uuid"))
                .andExpect(status().isBadRequest());
    }

    // --- retrait -------------------------------------------------------------

    @Test
    @WithMockUser
    void retrait_retourne204() throws Exception {
        doNothing().when(service).deleteForAction(CAPA, ACTION, EVIDENCE, null);

        mockMvc.perform(delete(UPLOAD + "/{evidenceId}", CAPA, ACTION, EVIDENCE).with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).deleteForAction(CAPA, ACTION, EVIDENCE, null);
    }

    @Test
    @WithMockUser
    void retrait_dUnePieceQuiNEstPasCelleDeLAction_retourne404() throws Exception {
        doThrow(new CapaEvidenceNotFoundException(EVIDENCE))
                .when(service).deleteForAction(CAPA, ACTION, EVIDENCE, null);

        mockMvc.perform(delete(UPLOAD + "/{evidenceId}", CAPA, ACTION, EVIDENCE).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("CAPA Evidence Not Found"));
    }

    // --- auteur du dépôt -----------------------------------------------------
    // Une preuve anonyme se défend mal devant un auditeur, mais un auteur
    // INVENTÉ se défend plus mal encore : la question « qui l'a produite ? »
    // vient toujours, et un audit prendrait la réponse pour argent comptant.

    @Test
    void depot_avecUnJeton_retientLeSujetCommeAuteur() throws Exception {
        UUID sujet = UUID.randomUUID();
        when(service.uploadForAction(any(), any(), any(), any(), any(), eq(sujet)))
                .thenReturn(new CapaEvidenceDto.Response(EVIDENCE, CAPA, ACTION, "application/pdf",
                        10L, "constat.pdf", sujet, Instant.parse("2026-08-09T10:00:00Z")));

        mockMvc.perform(multipart(UPLOAD, CAPA, ACTION).file(pdf())
                        .with(jwt().jwt(j -> j.subject(sujet.toString())))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(service).uploadForAction(eq(CAPA), eq(ACTION), any(), any(), any(), eq(sujet));
    }

    @Test
    void depot_avecUnSujetQuiNEstPasUnUuid_neFabriquePasDAuteur() throws Exception {
        when(service.uploadForAction(any(), any(), any(), any(), any(), any()))
                .thenReturn(new CapaEvidenceDto.Response(EVIDENCE, CAPA, ACTION, "application/pdf",
                        10L, "constat.pdf", null, Instant.parse("2026-08-09T10:00:00Z")));

        mockMvc.perform(multipart(UPLOAD, CAPA, ACTION).file(pdf())
                        .with(jwt().jwt(j -> j.subject("service-account-edge")))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(service).uploadForAction(eq(CAPA), eq(ACTION), any(), any(), any(), eq(null));
    }

    @Test
    void retrait_avecUnJeton_retientLeSujetCommeAuteurDuRetrait() throws Exception {
        UUID sujet = UUID.randomUUID();
        doNothing().when(service).deleteForAction(CAPA, ACTION, EVIDENCE, sujet);

        mockMvc.perform(delete(UPLOAD + "/{evidenceId}", CAPA, ACTION, EVIDENCE)
                        .with(jwt().jwt(j -> j.subject(sujet.toString())))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).deleteForAction(CAPA, ACTION, EVIDENCE, sujet);
    }

    @Test
    @WithMockUser
    void retrait_surUnDossierClos_retourne409() throws Exception {
        doThrow(new CapaStateException("Cannot change evidence on a CLOSED CAPA"))
                .when(service).deleteForAction(CAPA, ACTION, EVIDENCE, null);

        mockMvc.perform(delete(UPLOAD + "/{evidenceId}", CAPA, ACTION, EVIDENCE).with(csrf()))
                .andExpect(status().isConflict());
    }
}
