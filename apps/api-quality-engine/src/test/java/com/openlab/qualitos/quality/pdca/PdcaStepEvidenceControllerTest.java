package com.openlab.qualitos.quality.pdca;

import com.openlab.qualitos.quality.config.SecurityConfig;
import com.openlab.qualitos.quality.nonconformity.storage.StorageDisabledException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
 * Façade HTTP des preuves d'étape PDCA (§3.1, ADR 0061).
 *
 * <p>Ce qui se teste ici, c'est que la colonne « Preuve » du tableau parle un
 * langage de refus prévisible : 400 quand la pièce est refusée à l'entrée, 404
 * quand le cycle ou l'étape n'existe pas pour ce tenant, 409 quand l'étape porte
 * déjà sa pièce ou que le cycle est clos, 413 quand elle est trop lourde, 503
 * quand le stockage est coupé.
 *
 * <p>La {@link SecurityConfig} réelle est importée : sans elle, la tranche web
 * retombe sur la sécurité par défaut de Spring Boot — tout authentifié passe —
 * et le test qui vérifie que le retrait d'une preuve exige un manager qualité
 * passerait au vert sans rien prouver.
 */
@Tag("web")
@WebMvcTest(controllers = PdcaStepEvidenceController.class)
@Import(SecurityConfig.class)
class PdcaStepEvidenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PdcaStepEvidenceService service;

    /** Les tests posent le principal via {@code @WithMockUser} / {@code jwt()} : le décodeur n'est jamais sollicité. */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static final UUID CYCLE = UUID.randomUUID();
    private static final UUID STEP = UUID.randomUUID();
    private static final UUID EVIDENCE = UUID.randomUUID();

    private static final String UPLOAD = "/api/v1/pdca/cycles/{id}/steps/{stepId}/evidences";
    private static final String LIST = "/api/v1/pdca/cycles/{id}/step-evidences";

    private MockMultipartFile pdf() {
        return new MockMultipartFile("file", "constat.pdf", "application/pdf", "%PDF-1.7 x".getBytes());
    }

    // --- dépôt ---------------------------------------------------------------

    @Test
    @WithMockUser
    void depot_retourne201_etPorteLEtapeVisee() throws Exception {
        when(service.upload(eq(CYCLE), eq(STEP), eq("application/pdf"), eq("constat.pdf"), any(), any()))
                .thenReturn(new PdcaStepEvidenceDto.Response(EVIDENCE, CYCLE, STEP, "application/pdf",
                        10L, "constat.pdf", null, Instant.parse("2026-08-27T10:00:00Z")));

        mockMvc.perform(multipart(UPLOAD, CYCLE, STEP).file(pdf()).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(EVIDENCE.toString()))
                .andExpect(jsonPath("$.stepId").value(STEP.toString()))
                .andExpect(jsonPath("$.cycleId").value(CYCLE.toString()));
    }

    @Test
    @WithMockUser
    void depot_sansFichier_retourne400() throws Exception {
        MockMultipartFile vide = new MockMultipartFile("file", "vide.pdf", "application/pdf", new byte[0]);

        mockMvc.perform(multipart(UPLOAD, CYCLE, STEP).file(vide).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void depot_typeRefuse_retourne400() throws Exception {
        when(service.upload(any(), any(), any(), any(), any(), any()))
                .thenThrow(new PdcaStepEvidenceValidationException("Unsupported content type"));

        mockMvc.perform(multipart(UPLOAD, CYCLE, STEP).file(pdf()).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid PDCA Step Evidence"));
    }

    @Test
    @WithMockUser
    void depot_pieceTropLourde_retourne413() throws Exception {
        when(service.upload(any(), any(), any(), any(), any(), any()))
                .thenThrow(new PdcaStepEvidenceTooLargeException(20_000_000L, 10_485_760L));

        mockMvc.perform(multipart(UPLOAD, CYCLE, STEP).file(pdf()).with(csrf()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.title").value("PDCA Step Evidence Too Large"));
    }

    @Test
    @WithMockUser
    void depot_surUneEtapeInconnue_retourne404() throws Exception {
        when(service.upload(any(), any(), any(), any(), any(), any()))
                .thenThrow(new PdcaStepNotFoundException(STEP));

        mockMvc.perform(multipart(UPLOAD, CYCLE, STEP).file(pdf()).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void depot_surUneEtapeDejaPourvue_retourne409() throws Exception {
        when(service.upload(any(), any(), any(), any(), any(), any()))
                .thenThrow(new PdcaStateException("This step already carries its evidence file"));

        mockMvc.perform(multipart(UPLOAD, CYCLE, STEP).file(pdf()).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser
    void depot_stockageCoupe_retourne503() throws Exception {
        when(service.upload(any(), any(), any(), any(), any(), any()))
                .thenThrow(new StorageDisabledException());

        mockMvc.perform(multipart(UPLOAD, CYCLE, STEP).file(pdf()).with(csrf()))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void depot_sansJeton_retourne401() throws Exception {
        mockMvc.perform(multipart(UPLOAD, CYCLE, STEP).file(pdf()).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Le sujet du jeton devient l'auteur du dépôt : c'est ce qui permet à
     * l'auditeur de répondre à « qui a produit cette pièce ? ».
     */
    @Test
    void depot_prendLAuteurDansLeSujetDuJeton() throws Exception {
        UUID sujet = UUID.randomUUID();
        when(service.upload(any(), any(), any(), any(), any(), eq(sujet)))
                .thenReturn(new PdcaStepEvidenceDto.Response(EVIDENCE, CYCLE, STEP, "application/pdf",
                        10L, "constat.pdf", sujet, Instant.now()));

        mockMvc.perform(multipart(UPLOAD, CYCLE, STEP).file(pdf())
                        .with(jwt().jwt(j -> j.subject(sujet.toString())))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(service).upload(eq(CYCLE), eq(STEP), eq("application/pdf"), eq("constat.pdf"),
                any(), eq(sujet));
    }

    /**
     * Un jeton sans sujet du tout — jeton de service mal formé, échange de
     * jetons qui a perdu la revendication — ne fabrique pas d'auteur non plus.
     * Le cas est distinct du sujet non-UUID : il n'y a rien à convertir.
     */
    @Test
    void depot_jetonSansSujet_neFabriquePasDAuteur() throws Exception {
        when(service.upload(any(), any(), any(), any(), any(), isNull()))
                .thenReturn(new PdcaStepEvidenceDto.Response(EVIDENCE, CYCLE, STEP, "application/pdf",
                        10L, "constat.pdf", null, Instant.now()));

        mockMvc.perform(multipart(UPLOAD, CYCLE, STEP).file(pdf())
                        .with(jwt().jwt(j -> j.claims(c -> c.remove("sub"))))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(service).upload(any(), any(), any(), any(), any(), isNull());
    }

    /**
     * Part absente plutôt que vide.
     *
     * <p>Spring refuse déjà la requête avant d'entrer dans la méthode quand la
     * part {@code file} manque ; le garde-fou du contrôleur couvre l'appel
     * direct — un autre point d'entrée, un test, un futur client interne — et il
     * doit refuser de la même façon, sinon un {@code null} traverserait jusqu'au
     * service.
     */
    @Test
    void depot_partAbsente_estRefuseMemeEnAppelDirect() {
        PdcaStepEvidenceController controller = new PdcaStepEvidenceController(service);

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> controller.upload(CYCLE, STEP, null, null))
                .isInstanceOf(PdcaStepEvidenceValidationException.class)
                .hasMessageContaining("Missing or empty 'file' part");
    }

    /**
     * Un sujet qui n'est pas un UUID ne fabrique pas d'auteur : mieux vaut une
     * preuve sans auteur qu'un auteur inventé, qu'un audit prendrait pour argent
     * comptant.
     */
    @Test
    void depot_sujetNonUuid_neFabriquePasDAuteur() throws Exception {
        when(service.upload(any(), any(), any(), any(), any(), isNull()))
                .thenReturn(new PdcaStepEvidenceDto.Response(EVIDENCE, CYCLE, STEP, "application/pdf",
                        10L, "constat.pdf", null, Instant.now()));

        mockMvc.perform(multipart(UPLOAD, CYCLE, STEP).file(pdf())
                        .with(jwt().jwt(j -> j.subject("service-account-engine")))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(service).upload(eq(CYCLE), eq(STEP), eq("application/pdf"), eq("constat.pdf"),
                any(), isNull());
    }

    // --- liste ---------------------------------------------------------------

    @Test
    @WithMockUser
    void liste_rendLesPiecesDeToutesLesEtapes() throws Exception {
        when(service.listForCycle(CYCLE)).thenReturn(List.of(
                new PdcaStepEvidenceDto.ListItem(EVIDENCE, CYCLE, STEP, "application/pdf", 10L,
                        "constat.pdf", null, Instant.parse("2026-08-27T10:00:00Z"),
                        "https://minio.local/constat.pdf?sig=x")));

        mockMvc.perform(get(LIST, CYCLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stepId").value(STEP.toString()))
                .andExpect(jsonPath("$[0].url").value("https://minio.local/constat.pdf?sig=x"));
    }

    @Test
    @WithMockUser
    void liste_surUnCycleInconnu_retourne404() throws Exception {
        when(service.listForCycle(CYCLE)).thenThrow(new PdcaCycleNotFoundException(CYCLE));

        mockMvc.perform(get(LIST, CYCLE)).andExpect(status().isNotFound());
    }

    @Test
    void liste_sansJeton_retourne401() throws Exception {
        mockMvc.perform(get(LIST, CYCLE)).andExpect(status().isUnauthorized());
    }

    // --- retrait -------------------------------------------------------------

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void retrait_retourne204() throws Exception {
        doNothing().when(service).delete(eq(CYCLE), eq(STEP), eq(EVIDENCE), any());

        mockMvc.perform(delete(UPLOAD + "/{evidenceId}", CYCLE, STEP, EVIDENCE).with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).delete(eq(CYCLE), eq(STEP), eq(EVIDENCE), any());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void retrait_pieceInconnue_retourne404() throws Exception {
        doThrow(new PdcaStepEvidenceNotFoundException(EVIDENCE))
                .when(service).delete(any(), any(), any(), any());

        mockMvc.perform(delete(UPLOAD + "/{evidenceId}", CYCLE, STEP, EVIDENCE).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("PDCA Step Evidence Not Found"));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void retrait_surUnCycleClos_retourne409() throws Exception {
        doThrow(new PdcaStateException("Cannot change evidence on a COMPLETED cycle"))
                .when(service).delete(any(), any(), any(), any());

        mockMvc.perform(delete(UPLOAD + "/{evidenceId}", CYCLE, STEP, EVIDENCE).with(csrf()))
                .andExpect(status().isConflict());
    }

    /**
     * La suppression d'une preuve tombe sous la règle DELETE générique du socle :
     * Manager Qualité ou plus. Un opérateur simple verse une pièce, il ne la
     * retire pas — retirer une preuve d'un dossier d'audit n'est pas un geste de
     * saisie.
     */
    @Test
    @WithMockUser(roles = "USER")
    void retrait_parUnUtilisateurSimple_retourne403() throws Exception {
        mockMvc.perform(delete(UPLOAD + "/{evidenceId}", CYCLE, STEP, EVIDENCE).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
