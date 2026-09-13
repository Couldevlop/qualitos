package com.openlab.qualitos.quality.nonconformity.eightd.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.common.StepUpGuard;
import com.openlab.qualitos.quality.common.StepUpRequiredException;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDDto;
import com.openlab.qualitos.quality.nonconformity.eightd.application.EightDService;
import com.openlab.qualitos.quality.nonconformity.eightd.domain.EightDStateException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le partage des rôles sur le rapport 8D.
 *
 * <p>LIRE est ouvert à tout utilisateur authentifié — y compris le PDF : l'opérateur
 * qui a signalé le défaut et l'auditeur interne doivent pouvoir lire la suite qu'on a
 * donnée à l'écart, sans habilitation d'écriture.
 *
 * <p>ÉCRIRE ne l'est pas, et ÉMETTRE exige en plus un second facteur : l'émission
 * produit un document signé et ancré, remis au client (§18.2 #5).
 */
@Tag("web")
@WebMvcTest(controllers = EightDController.class)
@Import(MethodSecurityTestConfig.class)
class EightDControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean EightDService service;

    /**
     * La garde est doublée ici : ce banc vérifie le partage des rôles et le mapping
     * HTTP. Ce que la garde lit dans le jeton est vérifié par {@code StepUpAuthenticationTest}.
     */
    @MockitoBean StepUpGuard stepUp;

    private static final UUID NC = UUID.randomUUID();
    private static final String URL = "/api/v1/nc/" + NC + "/8d";
    private static final Instant NOW = Instant.parse("2026-09-13T10:00:00Z");

    private static final String CORPS = """
            {"team":"Ada, Grace","containment":"Tri à 100 % du stock","recognition":"Merci"}""";

    private static EightDDto.ReportView vue(boolean emis) {
        return new EightDDto.ReportView(
                NC, "NC-2026-0007", "Fuite au presse-étoupe", emis ? "ISSUED" : "DRAFT",
                !emis, true, List.of("D7"), "Ada, Grace", "Tri à 100 %", "Merci",
                List.of(new EightDDto.DisciplineView("D1", "Équipe", true,
                        "Saisi par l'équipe qualité", List.of("Ada, Grace"), true)),
                emis ? new EightDDto.SealView("d".repeat(64), "tx-1", "ABCDEFGHIJKLMNOP",
                        NOW, "Ada Lovelace") : null);
    }

    // ---------- lire ----------

    @Test
    @WithMockUser(roles = "USER")
    void un_utilisateur_simple_peut_lire_le_rapport() throws Exception {
        when(service.consulter(NC)).thenReturn(vue(false));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ncReference").value("NC-2026-0007"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.missingCodes[0]").value("D7"))
                .andExpect(jsonPath("$.disciplines[0].editable").value(true));
    }

    @Test
    @WithAnonymousUser
    void un_anonyme_ne_lit_rien() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "USER")
    void un_utilisateur_simple_peut_telecharger_le_pdf() throws Exception {
        when(service.pdf(NC)).thenReturn(new EightDDto.PdfResult(
                "%PDF-1.6 factice".getBytes(StandardCharsets.ISO_8859_1),
                "8d-nc-2026-0007-20260913-100000.pdf", "d".repeat(64), "ABCDEFGHIJKLMNOP"));

        mockMvc.perform(get(URL + "/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                // Le nom vient du SERVEUR : le front ne le refabrique pas.
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("8d-nc-2026-0007-20260913-100000.pdf")))
                .andExpect(header().string("X-EightD-Sha256", "d".repeat(64)))
                .andExpect(header().string("X-EightD-Verification-Code", "ABCDEFGHIJKLMNOP"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void un_pdf_non_encore_emis_repond_409() throws Exception {
        when(service.pdf(NC)).thenThrow(new EightDStateException("Le rapport 8D n'est pas encore émis"));

        mockMvc.perform(get(URL + "/pdf")).andExpect(status().isConflict());
    }

    // ---------- écrire ----------

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void le_manager_qualite_renseigne_les_trois_disciplines_saisies() throws Exception {
        when(service.enregistrer(eq(NC), any())).thenReturn(vue(false));

        mockMvc.perform(put(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.team").value("Ada, Grace"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void un_utilisateur_simple_ne_renseigne_rien() throws Exception {
        mockMvc.perform(put(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "USER")
    void le_refus_precede_la_lecture_du_corps() throws Exception {
        // ADR 0065 : avec un corps illisible ET un rôle insuffisant, la réponse est 403
        // et non 400. Sans cela, le point d'entrée fermé servirait d'oracle de validation.
        mockMvc.perform(put(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{ceci n'est pas du json"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "USER")
    void un_utilisateur_simple_n_emet_pas() throws Exception {
        mockMvc.perform(post(URL + "/issue").with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service, stepUp);
    }

    @Test
    @WithMockUser(roles = "DIRECTOR_QUALITY")
    void le_directeur_qualite_emet_le_rapport() throws Exception {
        when(service.emettre(NC)).thenReturn(vue(true));

        mockMvc.perform(post(URL + "/issue").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ISSUED"))
                .andExpect(jsonPath("$.seal.anchorTxRef").value("tx-1"))
                .andExpect(jsonPath("$.seal.verificationCode").value("ABCDEFGHIJKLMNOP"));

        verify(stepUp).require("émettre un rapport 8D");
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void sans_second_facteur_l_emission_est_refusee_et_rien_n_est_signe() throws Exception {
        doThrow(new StepUpRequiredException("émettre un rapport 8D")).when(stepUp).require(any());

        mockMvc.perform(post(URL + "/issue").with(csrf()))
                .andExpect(status().is4xxClientError());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void emettre_avant_la_cloture_repond_409() throws Exception {
        when(service.emettre(NC)).thenThrow(new EightDStateException(
                "Un rapport 8D ne s'émet qu'à la clôture de la non-conformité"));

        mockMvc.perform(post(URL + "/issue").with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void l_admin_du_tenant_renseigne_aussi() throws Exception {
        when(service.enregistrer(eq(NC), any())).thenReturn(vue(false));

        mockMvc.perform(put(URL).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isOk());
    }
}
