package com.openlab.qualitos.quality.fives;

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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La porte des photos de terrain.
 *
 * <p>Ce qu'elle doit tenir : le pilier se lit dans le CHEMIN (c'est ce qui rend
 * l'envoi rejouable depuis la file hors ligne), l'acteur se lit dans le jeton,
 * et un opérateur simple peut verser — c'est une saisie de terrain, pas un acte
 * de pilotage.
 */
@Tag("web")
@WebMvcTest(controllers = FiveSItemPhotoController.class)
class FiveSItemPhotoControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FiveSItemPhotoService service;

    static final UUID AUDIT = UUID.randomUUID();
    static final UUID PHOTO = UUID.randomUUID();

    private static MockMultipartFile partJpeg() {
        byte[] jpeg = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x11 };
        return new MockMultipartFile("image", "zone.jpg", "image/jpeg", jpeg);
    }

    @Test @WithMockUser
    void depot_renvoie201EtLitLePilierDansLeChemin() throws Exception {
        when(service.upload(eq(AUDIT), eq(FiveSPillar.SEIRI), any(), any(), any(), any()))
                .thenReturn(reponse(FiveSPillar.SEIRI));

        mockMvc.perform(multipart("/api/v1/fives/audits/" + AUDIT + "/photos/SEIRI")
                        .file(partJpeg()).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pillar").value("SEIRI"))
                .andExpect(jsonPath("$.auditId").value(AUDIT.toString()));
    }

    /**
     * Un opérateur simple parcourt l'atelier : réserver la prise de vue au
     * pilotage qualité supprimerait la fonction.
     */
    @Test @WithMockUser(roles = "USER")
    void depot_ouvertAUnOperateurSimple() throws Exception {
        when(service.upload(eq(AUDIT), eq(FiveSPillar.SEISO), any(), any(), any(), any()))
                .thenReturn(reponse(FiveSPillar.SEISO));

        mockMvc.perform(multipart("/api/v1/fives/audits/" + AUDIT + "/photos/SEISO")
                        .file(partJpeg()).with(csrf()))
                .andExpect(status().isCreated());
    }

    @Test @WithMockUser
    void depot_sansImage_refuse() throws Exception {
        mockMvc.perform(multipart("/api/v1/fives/audits/" + AUDIT + "/photos/SEIRI")
                        .file(new MockMultipartFile("image", "vide.jpg", "image/jpeg", new byte[0]))
                        .with(csrf()))
                .andExpect(status().isUnprocessableEntity());

        verify(service, never()).upload(any(), any(), any(), any(), any(), any());
    }

    @Test @WithMockUser
    void depot_pilierInconnu_refuse() throws Exception {
        mockMvc.perform(multipart("/api/v1/fives/audits/" + AUDIT + "/photos/SEIRO")
                        .file(partJpeg()).with(csrf()))
                .andExpect(status().isBadRequest());

        verify(service, never()).upload(any(), any(), any(), any(), any(), any());
    }

    @Test @WithMockUser
    void depot_pilierNonNote_renvoie409() throws Exception {
        when(service.upload(eq(AUDIT), eq(FiveSPillar.SHITSUKE), any(), any(), any(), any()))
                .thenThrow(new FiveSStateException("Pillar SHITSUKE has no score on this audit"));

        mockMvc.perform(multipart("/api/v1/fives/audits/" + AUDIT + "/photos/SHITSUKE")
                        .file(partJpeg()).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test @WithMockUser
    void depot_tropLourde_renvoie413() throws Exception {
        when(service.upload(eq(AUDIT), eq(FiveSPillar.SEIRI), any(), any(), any(), any()))
                .thenThrow(new FiveSItemPhotoTooLargeException("A single photo cannot exceed 10 MB"));

        mockMvc.perform(multipart("/api/v1/fives/audits/" + AUDIT + "/photos/SEIRI")
                        .file(partJpeg()).with(csrf()))
                .andExpect(status().isPayloadTooLarge());
    }

    @Test @WithMockUser
    void liste_renvoieLesVignettesDeLaudit() throws Exception {
        when(service.listForAudit(AUDIT)).thenReturn(List.of(element(FiveSPillar.SEIRI)));

        mockMvc.perform(get("/api/v1/fives/audits/" + AUDIT + "/photos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].url").value("https://objets.test/signee"));
    }

    @Test @WithMockUser
    void liste_filtreeParPilier() throws Exception {
        when(service.listForPillar(AUDIT, FiveSPillar.SEITON))
                .thenReturn(List.of(element(FiveSPillar.SEITON)));

        mockMvc.perform(get("/api/v1/fives/audits/" + AUDIT + "/photos").param("pillar", "SEITON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pillar").value("SEITON"));

        verify(service, never()).listForAudit(any());
    }

    @Test @WithMockUser
    void retrait_renvoie204() throws Exception {
        mockMvc.perform(delete("/api/v1/fives/audits/" + AUDIT + "/photos/" + PHOTO).with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).delete(eq(AUDIT), eq(PHOTO), any());
    }

    @Test @WithMockUser
    void retrait_photoInconnue_renvoie404() throws Exception {
        org.mockito.Mockito.doThrow(new FiveSItemPhotoNotFoundException(PHOTO))
                .when(service).delete(eq(AUDIT), eq(PHOTO), any());

        mockMvc.perform(delete("/api/v1/fives/audits/" + AUDIT + "/photos/" + PHOTO).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void sansJeton_rienNestVisible() throws Exception {
        mockMvc.perform(get("/api/v1/fives/audits/" + AUDIT + "/photos"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- fixtures ----------

    private static FiveSItemPhotoDto.Response reponse(FiveSPillar pilier) {
        return new FiveSItemPhotoDto.Response(UUID.randomUUID(), AUDIT, pilier, "image/jpeg",
                1024L, "zone.jpg", UUID.randomUUID(), Instant.parse("2026-11-09T10:00:00Z"));
    }

    private static FiveSItemPhotoDto.ListItem element(FiveSPillar pilier) {
        return new FiveSItemPhotoDto.ListItem(UUID.randomUUID(), AUDIT, pilier, "image/jpeg",
                1024L, "zone.jpg", UUID.randomUUID(), Instant.parse("2026-11-09T10:00:00Z"),
                "https://objets.test/signee");
    }
}
