package com.openlab.qualitos.quality.settings.catalog;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Les listes déroulantes se lisent par tout authentifié — sans la liste, pas de
 * saisie — et ne se changent que par l'administrateur du client.
 */
@Tag("web")
@WebMvcTest(controllers = TenantCatalogController.class)
@Import(MethodSecurityTestConfig.class)
class TenantCatalogControllerTest {

    static final String BASE = "/api/v1/settings/catalogs";

    static final String CORPS = "{\"entries\":[{\"code\":\"BLOC-A\",\"label\":\"Bloc A\"}]}";

    @Autowired MockMvc mockMvc;
    @MockitoBean TenantCatalogService service;

    private static TenantCatalogService.View defauts(CatalogId id) {
        return new TenantCatalogService.View(id, true, id.defaults());
    }

    @Test
    @WithMockUser(roles = "USER")
    void toutAuthentifieLitUneListeEtSonOrigine() throws Exception {
        when(service.view(CatalogId.AUDIT_TYPE)).thenReturn(defauts(CatalogId.AUDIT_TYPE));

        mockMvc.perform(get(BASE + "/audit-type"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.catalogue").value("audit-type"))
                .andExpect(jsonPath("$.mode").value("RENAME"))
                .andExpect(jsonPath("$.defaults").value(true))
                .andExpect(jsonPath("$.entries[0].code").value("INTERNAL"))
                .andExpect(jsonPath("$.entries[0].label").value("Interne"))
                .andExpect(jsonPath("$.entries[0].active").value(true));
    }

    @Test
    @WithMockUser(roles = "USER")
    void toutAuthentifieListeLesCataloguesOuverts() throws Exception {
        when(service.summaries()).thenReturn(List.of(
                new TenantCatalogService.Summary("fives-zone", CatalogMode.OPEN, false, 8)));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].catalogue").value("fives-zone"))
                .andExpect(jsonPath("$[0].mode").value("OPEN"))
                .andExpect(jsonPath("$[0].defaults").value(false))
                .andExpect(jsonPath("$[0].entryCount").value(8));
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void lAdministrateurRemplaceEtRetablit() throws Exception {
        when(service.replace(eq(CatalogId.FIVES_ZONE), anyList())).thenReturn(
                new TenantCatalogService.View(CatalogId.FIVES_ZONE, false,
                        List.of(CatalogEntry.of("BLOC-A", "Bloc A"))));
        when(service.reset(CatalogId.FIVES_ZONE)).thenReturn(defauts(CatalogId.FIVES_ZONE));

        mockMvc.perform(put(BASE + "/fives-zone").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaults").value(false))
                .andExpect(jsonPath("$.entries[0].code").value("BLOC-A"));
        mockMvc.perform(post(BASE + "/fives-zone/reset").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.defaults").value(true));

        // Une entrée sans « active » est active : l'écran n'a pas à le dire pour
        // le cas de loin le plus courant.
        verify(service).replace(eq(CatalogId.FIVES_ZONE), argThat(e ->
                e.size() == 1 && e.get(0).code().equals("BLOC-A") && e.get(0).active()));
        verify(service).reset(CatalogId.FIVES_ZONE);
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void uneEntreeDesactiveeArriveDesactivee() throws Exception {
        when(service.replace(eq(CatalogId.FIVES_ZONE), anyList())).thenReturn(
                new TenantCatalogService.View(CatalogId.FIVES_ZONE, false,
                        List.of(new CatalogEntry("BLOC-A", "Bloc A", false),
                                CatalogEntry.of("BLOC-B", "Bloc B"))));

        mockMvc.perform(put(BASE + "/fives-zone").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"code\":\"BLOC-A\",\"label\":\"Bloc A\","
                                + "\"active\":false},{\"code\":\"BLOC-B\",\"label\":\"Bloc B\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries[0].active").value(false));

        verify(service).replace(eq(CatalogId.FIVES_ZONE), argThat(e -> !e.get(0).active()));
    }

    @Test
    @WithMockUser(roles = {"QUALITY_DIRECTOR", "QUALITY_MANAGER"})
    void lePilotageNeParametrePas() throws Exception {
        mockMvc.perform(put(BASE + "/fives-zone").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(BASE + "/fives-zone/reset").with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void sansJeton_rienNEstLu() throws Exception {
        mockMvc.perform(get(BASE + "/fives-zone")).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void uneListeInconnueEst404_etNeToucheRien() throws Exception {
        mockMvc.perform(get(BASE + "/liste-qui-nexiste-pas"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(BASE + "/liste-qui-nexiste-pas").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CORPS))
                .andExpect(status().isNotFound());

        verify(service, org.mockito.Mockito.never()).replace(any(), anyList());
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void uneListeRefuseeParLeDomaineEst422() throws Exception {
        when(service.replace(eq(CatalogId.AUDIT_TYPE), anyList()))
                .thenThrow(new CatalogValidationException("Unknown code 'CROISE' for catalogue audit-type"));

        mockMvc.perform(put(BASE + "/audit-type").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"entries\":[{\"code\":\"CROISE\",\"label\":\"Croisé\"}]}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void unCorpsMalFormeEstRefuseAvantLeService() throws Exception {
        for (String c : List.of(
                "{\"entries\":[]}",
                "{}",
                CORPS.replace("\"label\":\"Bloc A\"", "\"label\":\"\""),
                CORPS.replace("\"code\":\"BLOC-A\"", "\"code\":\"   \""))) {
            mockMvc.perform(put(BASE + "/fives-zone").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(c))
                    .andExpect(status().isBadRequest());
        }

        verifyNoInteractions(service);
    }
}
