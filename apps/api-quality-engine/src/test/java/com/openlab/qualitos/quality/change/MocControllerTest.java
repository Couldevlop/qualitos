package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.change.domain.ImpactDomain;
import com.openlab.qualitos.quality.change.domain.MocClassification;
import com.openlab.qualitos.quality.change.domain.MocException;
import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le contrat HTTP du MOC : droits livrés (demander, analyser, paramétrer),
 * validation des corps, traduction des refus en statuts et codes stables.
 */
@Tag("web")
@WebMvcTest(controllers = {MocController.class, MocSettingsController.class})
@Import(MethodSecurityTestConfig.class)
class MocControllerTest {

    static final String BASE = "/api/v1/changes";
    static final UUID ID = UUID.randomUUID();
    static final UUID CARTE = UUID.randomUUID();
    static final String DEMANDE = "{\"title\":\"Four\",\"type\":\"PERMANENT\",\"site\":\"Atelier 2\"}";
    static final String PARAMETRES = "{\"majorThreshold\":25,\"criticalThreshold\":50,\"reminderDays\":3,"
            + "\"weights\":{\"SAFETY\":1,\"HEALTH_ENVIRONMENT\":1,\"QUALITY\":1,\"PRODUCTION\":1,"
            + "\"MAINTENANCE\":1,\"COSTS\":1,\"REGULATORY\":1,\"TRAINING\":1},"
            + "\"roles\":[{\"label\":\"HSE\",\"requiredFor\":[\"MINOR\",\"MAJOR\",\"CRITICAL\"],"
            + "\"holders\":[{\"userId\":\"" + UUID.randomUUID() + "\",\"label\":\"j@acme.test\"}]}]}";

    @Autowired MockMvc mockMvc;
    @MockitoBean MocService service;
    @MockitoBean MocSettingsService settings;

    static MocDto.Detail detail() {
        return new MocDto.Detail(ID, "MOC-2026-014", "Four", ChangeRequestType.PERMANENT, "Atelier 2", null, null,
                null, ChangeRequestStatus.DRAFT, "S. Benali", true, true, Instant.now(), null, null, null,
                MocClassification.NOT_ASSESSED, List.of(), List.of(), List.of());
    }

    @Test
    @WithMockUser(roles = "USER")
    void unUtilisateurLitDemandeEtConduitSesDemandes() throws Exception {
        when(service.list()).thenReturn(List.of(new MocDto.Summary(ID, "MOC-2026-014", "Four",
                ChangeRequestType.PERMANENT, ChangeRequestStatus.DRAFT, MocClassification.NOT_ASSESSED, null,
                "S. Benali", Instant.now(), false)));
        when(service.create(any())).thenReturn(detail());
        when(service.get(ID)).thenReturn(detail());

        mockMvc.perform(get(BASE)).andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value("MOC-2026-014"));
        mockMvc.perform(get(BASE + "/" + ID)).andExpect(status().isOk());
        mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(DEMANDE))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("DRAFT"));
        mockMvc.perform(post(BASE + "/" + ID + "/analysis").with(csrf())).andExpect(status().isOk());
        mockMvc.perform(delete(BASE + "/" + ID).with(csrf())).andExpect(status().isNoContent());

        // Le service vérifie qu'il conduit CETTE demande : un refus y devient un 403 nommé.
        when(service.submit(ID)).thenThrow(new MocException(MocException.Kind.FORBIDDEN, "moc.not-owner", "x"));
        mockMvc.perform(post(BASE + "/" + ID + "/submit").with(csrf()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("moc.not-owner"));
    }

    @Test
    @WithMockUser(roles = "AUDITOR")
    void unAuditeurLitMaisNeDemandeRien() throws Exception {
        when(service.list()).thenReturn(List.of());
        mockMvc.perform(get(BASE)).andExpect(status().isOk());
        mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(DEMANDE))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/" + ID + "/submit").with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/" + ID + "/reopen").with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/" + ID + "/impacts/SAFETY").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"level\":\"LOW\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/" + ID).with(csrf())).andExpect(status().isForbidden());
        verify(service, never()).create(any());
        verify(service, never()).submit(any());
        verify(service, never()).assess(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "QUALITY_MANAGER")
    void leManagerQualiteAnalyseEtSoumet() throws Exception {
        when(service.assess(eq(ID), eq(ImpactDomain.SAFETY), any())).thenReturn(detail());
        when(service.submit(ID)).thenReturn(detail());
        when(service.reopen(ID)).thenReturn(detail());
        when(service.update(eq(ID), any())).thenReturn(detail());
        mockMvc.perform(put(BASE + "/" + ID + "/impacts/SAFETY").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"level\":\"HIGH\",\"notes\":\"EPI\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post(BASE + "/" + ID + "/submit").with(csrf())).andExpect(status().isOk());
        mockMvc.perform(post(BASE + "/" + ID + "/reopen").with(csrf())).andExpect(status().isOk());
        mockMvc.perform(put(BASE + "/" + ID).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(DEMANDE))
                .andExpect(status().isOk());
        // Un domaine inconnu, un niveau absent : refusés à la forme.
        mockMvc.perform(put(BASE + "/" + ID + "/impacts/WEATHER").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"level\":\"HIGH\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(BASE + "/" + ID + "/impacts/SAFETY").with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    void uneDemandeMalFormeeEstRefusee() throws Exception {
        mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \",\"type\":\"PERMANENT\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Four\"}"))
                .andExpect(status().isBadRequest());
        verify(service, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void lesRefusDuMocPortentUnStatutEtUnCodeStable() throws Exception {
        when(service.decide(eq(ID), eq(CARTE), any())).thenThrow(new MocException(MocException.Kind.FORBIDDEN,
                "moc.not-holder", "Seul un titulaire…", Map.of("role", "HSE")));
        mockMvc.perform(post(BASE + "/" + ID + "/validations/" + CARTE + "/decision").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("moc.not-holder"))
                .andExpect(jsonPath("$.role").value("HSE"));

        when(service.sendToAnalysis(ID)).thenThrow(MocException.conflict("moc.not-draft", "x"));
        mockMvc.perform(post(BASE + "/" + ID + "/analysis").with(csrf()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("moc.not-draft"));

        when(service.get(ID)).thenThrow(new MocException(MocException.Kind.NOT_FOUND, "moc.validation-not-found", "x"));
        mockMvc.perform(get(BASE + "/" + ID)).andExpect(status().isNotFound());

        when(service.create(any())).thenThrow(MocException.invalid("moc.comment-required", "x"));
        mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(DEMANDE))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("moc.comment-required"));

        UUID absente = UUID.randomUUID();
        when(service.reopen(absente)).thenThrow(new ChangeRequestNotFoundException(absente));
        when(service.decide(eq(absente), any(), any())).thenThrow(new ChangeRequestNotFoundException(absente));
        mockMvc.perform(post(BASE + "/" + absente + "/validations/" + CARTE + "/decision").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"decision\":\"REJECTED\",\"comment\":\"Non\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    void lExportEstUnClasseurATelecharger() throws Exception {
        when(service.exportRows()).thenReturn(List.of());
        mockMvc.perform(get(BASE + "/export.xlsx"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", MocController.XLSX))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString(".xlsx")));
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void lAdministrateurLitEtRegleLesParametres() throws Exception {
        MocDto.SettingsView vue = new MocDto.SettingsView(25, 50, 3, Map.of(ImpactDomain.SAFETY, 1), List.of());
        when(settings.view()).thenReturn(vue);
        when(settings.replace(any())).thenReturn(vue);
        mockMvc.perform(get(BASE + "/settings")).andExpect(status().isOk())
                .andExpect(jsonPath("$.criticalThreshold").value(50));
        mockMvc.perform(put(BASE + "/settings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(PARAMETRES))
                .andExpect(status().isOk());
        // Bornes vérifiées à la forme : délai hors 1..30, poids hors 0..10.
        mockMvc.perform(put(BASE + "/settings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(PARAMETRES.replace("\"reminderDays\":3", "\"reminderDays\":31")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(BASE + "/settings").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(PARAMETRES.replace("\"SAFETY\":1", "\"SAFETY\":11")))
                .andExpect(status().isBadRequest());
        when(settings.replace(any())).thenThrow(new MocException(MocException.Kind.CONFLICT,
                "moc.settings.role-in-use", "x", Map.of("role", "HSE")));
        mockMvc.perform(put(BASE + "/settings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(PARAMETRES))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("moc.settings.role-in-use"));
    }

    @Test
    @WithMockUser(roles = "QUALITY_DIRECTOR")
    void leDirecteurQualiteNAccedePasAuxParametres() throws Exception {
        mockMvc.perform(get(BASE + "/settings")).andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/settings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isForbidden());
        verify(settings, never()).replace(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void unUtilisateurNAccedePasAuxParametres() throws Exception {
        mockMvc.perform(get(BASE + "/settings")).andExpect(status().isForbidden());
        mockMvc.perform(put(BASE + "/settings").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(PARAMETRES))
                .andExpect(status().isForbidden());
        verify(settings, never()).view();
    }

    @Test
    void sansAuthentificationRienNePasse() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }
}
