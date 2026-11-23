package com.openlab.qualitos.quality.derogation.web;

import com.openlab.qualitos.quality.authz.application.RecordScope;
import com.openlab.qualitos.quality.authz.domain.Permission;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.derogation.application.DerogationDto;
import com.openlab.qualitos.quality.derogation.application.DerogationMailException;
import com.openlab.qualitos.quality.derogation.application.DerogationService;
import com.openlab.qualitos.quality.derogation.application.DirectoryUnavailableException;
import com.openlab.qualitos.quality.derogation.domain.CustomerDecision;
import com.openlab.qualitos.quality.derogation.domain.DerogationConflictException;
import com.openlab.qualitos.quality.derogation.domain.DerogationNotFoundException;
import com.openlab.qualitos.quality.derogation.domain.DerogationStateException;
import com.openlab.qualitos.quality.derogation.domain.DerogationStatus;
import com.openlab.qualitos.quality.derogation.domain.DerogationValidationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La surface HTTP : le partage des droits, et le statut de chaque refus.
 *
 * <p>Ce banc vérifie ce que ni le domaine ni le service ne peuvent voir, parce
 * qu'ils vivent sous les annotations : qu'un auditeur lise mais n'écrive pas,
 * qu'un dossier hors de portée réponde <b>404 et non 403</b> (ADR 0081), et que
 * la fiche incomplète rende la LISTE des champs manquants — c'est elle qui
 * permet à l'écran de les marquer sans deviner.
 */
@Tag("web")
@WebMvcTest(controllers = DerogationController.class)
@Import(MethodSecurityTestConfig.class)
class DerogationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean DerogationService service;
    @MockitoBean RecordScope scope;

    private static final UUID ID = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID MANAGER = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant NOW = Instant.parse("2026-11-22T10:00:00Z");

    private static final DerogationDto.View VUE = new DerogationDto.View(
            ID, "DER-2026-0001", DerogationStatus.DRAFT, true,
            "ACME Industrie", null, null, "P-4420", null, null, null, null,
            null, null, null, null, null,
            List.of(), "Pieces fonctionnelles",
            null, null, null, null, null,
            null, null, null, null, null, null,
            null, null, null, null,
            CustomerDecision.PENDING, null,
            null, "Paul Girard", NOW, NOW);

    /**
     * La langue est OBLIGATOIRE à la frontière web.
     *
     * <p>Le service sait se rabattre sur le français, mais ce repli est un filet,
     * pas une tolérance : l'ADR 0129 veut qu'aucun envoi ne parte dans une langue
     * que personne n'a choisie, et c'est ce document-là que lit un tiers.
     */
    private static final String ENVOI =
            "{\"customerEmail\":\"qa@client.fr\",\"comment\":\"Lot urgent\","
                    + "\"language\":\"FR\"}";

    // ---------- le partage des droits ----------

    @Test
    @WithMockUser(roles = "AUDITOR")
    void unAuditeurLitLeRegistreMaisNOuvrePasDeDossier() throws Exception {
        when(scope.restrictTo(Permission.DEROGATION_VIEW_ALL)).thenReturn(Optional.empty());
        when(service.search(any(), anyBoolean())).thenReturn(
                new DerogationDto.PageView<>(List.of(), 0L, 0, 20));

        mockMvc.perform(get("/api/v1/derogations")).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/derogations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void unUtilisateurNEnvoiePasAuClient() throws Exception {
        // L'envoi fait partir un courriel depuis l'adresse de l'organisation :
        // il est reserve au pilotage, comme tous les gestes de l'initiateur.
        mockMvc.perform(post("/api/v1/derogations/{id}/send-to-customer", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ENVOI))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void leManagerQualiteMeneUnDossierDeBoutEnBout() throws Exception {
        when(scope.restrictTo(Permission.DEROGATION_VIEW_ALL)).thenReturn(Optional.empty());
        when(service.create(any())).thenReturn(VUE);
        when(service.requestApproval(eq(ID), any())).thenReturn(VUE);
        when(service.sendToCustomer(eq(ID), any())).thenReturn(VUE);

        mockMvc.perform(post("/api/v1/derogations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\":\"ACME Industrie\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").value("DER-2026-0001"));

        mockMvc.perform(post("/api/v1/derogations/{id}/request-approval", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerAccountId\":\"" + MANAGER + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/derogations/{id}/send-to-customer", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ENVOI))
                .andExpect(status().isOk());
    }

    @Test
    @WithAnonymousUser
    void sansJetonRienNEstAccessible() throws Exception {
        mockMvc.perform(get("/api/v1/derogations/{id}", ID))
                .andExpect(status().isUnauthorized());
    }

    // ---------- la portée du registre ----------

    @Test
    @WithMockUser(roles = "USER")
    void sansLeDroitDeToutVoirLaPorteeEstTransmiseAuService() throws Exception {
        // Le controleur traduit la restriction en un booleen : c'est le service
        // qui sait ce que « me concerner » veut dire pour une derogation.
        when(scope.restrictTo(Permission.DEROGATION_VIEW_ALL))
                .thenReturn(Optional.of(UUID.randomUUID()));
        when(service.get(eq(ID), eq(false))).thenReturn(VUE);

        mockMvc.perform(get("/api/v1/derogations/{id}", ID)).andExpect(status().isOk());
        verify(service).get(ID, false);
    }

    // ---------- les statuts de refus ----------

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unDossierHorsDePorteeRepond404EtNon403() throws Exception {
        // Repondre 403 revelerait son existence, et donc qu'un client donne a
        // demande une derogation (ADR 0081).
        when(scope.restrictTo(Permission.DEROGATION_VIEW_ALL)).thenReturn(Optional.empty());
        when(service.get(eq(ID), anyBoolean()))
                .thenThrow(new DerogationNotFoundException(ID));

        mockMvc.perform(get("/api/v1/derogations/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Derogation Not Found"));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneFicheIncompleteRend422AvecLaListeDesManquants() throws Exception {
        when(service.requestApproval(eq(ID), any())).thenThrow(
                new DerogationValidationException("Champs obligatoires manquants : client",
                        List.of("client", "quantité")));

        mockMvc.perform(post("/api/v1/derogations/{id}/request-approval", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerAccountId\":\"" + MANAGER + "\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.missingFields.length()").value(2))
                .andExpect(jsonPath("$.missingFields[0]").value("client"));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unEtatQuiNePermetPasLeGesteRend409() throws Exception {
        when(service.withdraw(eq(ID)))
                .thenThrow(new DerogationStateException("Ce dossier n'est pas en attente."));

        mockMvc.perform(post("/api/v1/derogations/{id}/withdraw", ID).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void deuxGestesQuiSeCroisentRendent409() throws Exception {
        when(service.update(eq(ID), any()))
                .thenThrow(new DerogationConflictException("Ce dossier a change.", new RuntimeException()));

        mockMvc.perform(put("/api/v1/derogations/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unCourrielQuiNePartPasRend502EtNonUneErreurDeSaisie() throws Exception {
        // La faute n'est pas a l'appelant, et le dossier reste renvoyable.
        when(service.sendToCustomer(eq(ID), any()))
                .thenThrow(new DerogationMailException("relais injoignable"));

        mockMvc.perform(post("/api/v1/derogations/{id}/send-to-customer", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ENVOI))
                .andExpect(status().isBadGateway());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unAnnuaireInjoignableRend502EtNonUnManagerInconnu() throws Exception {
        // Un compte absent et un annuaire muet ne se disent pas pareil : le
        // second ferait corriger une saisie correcte.
        when(service.requestApproval(eq(ID), any())).thenThrow(
                new DirectoryUnavailableException("annuaire muet", new RuntimeException()));

        mockMvc.perform(post("/api/v1/derogations/{id}/request-approval", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerAccountId\":\"" + MANAGER + "\"}"))
                .andExpect(status().isBadGateway());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void unEnvoiSansLangueChoisieEstRefuse() throws Exception {
        // Le document part a un TIERS : le laisser sortir dans une langue que
        // personne n'a choisie est precisement ce que l'ADR 0129 interdit.
        mockMvc.perform(post("/api/v1/derogations/{id}/send-to-customer", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerEmail\":\"qa@client.fr\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void uneAdresseDeClientMalFormeeEstRefusee() throws Exception {
        mockMvc.perform(post("/api/v1/derogations/{id}/send-to-customer", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerEmail\":\"pas-une-adresse\",\"language\":\"FR\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    // ---------- « Mes validations » ----------

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void lAlerteRougeRendSonNombre() throws Exception {
        when(service.myPendingApprovalCount())
                .thenReturn(new DerogationDto.PendingCount(3L));

        mockMvc.perform(get("/api/v1/derogations/my-approvals/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void mesValidationsNePassentPasParLaPorteeDuRegistre() throws Exception {
        // Ce qu'on doit signer nous concerne par definition : y appliquer la
        // restriction de visibilite serait redondant, et pourrait l'exclure.
        when(service.myPendingApprovals(0, 20))
                .thenReturn(new DerogationDto.PageView<>(List.of(), 0L, 0, 20));

        mockMvc.perform(get("/api/v1/derogations/my-approvals"))
                .andExpect(status().isOk());
        verifyNoInteractions(scope);
    }

    // ---------- le document ----------

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void lePdfSortEnPieceAfficheeEtNonEnTelechargement() throws Exception {
        // On le regarde AVANT de l'envoyer : c'est le seul moyen de verifier ce
        // qu'un tiers va lire.
        when(scope.restrictTo(Permission.DEROGATION_VIEW_ALL)).thenReturn(Optional.empty());
        when(service.renderPdf(eq(ID), anyBoolean())).thenReturn("%PDF-1.7 x".getBytes());

        mockMvc.perform(get("/api/v1/derogations/{id}/pdf", ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(result -> {
                    String disposition = result.getResponse().getHeader("Content-Disposition");
                    org.assertj.core.api.Assertions.assertThat(disposition)
                            .startsWith("inline").contains("derogation-" + ID + ".pdf");
                });
    }
}
