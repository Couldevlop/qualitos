package com.openlab.qualitos.quality.change;

import com.openlab.qualitos.quality.capa.CapaController;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.change.domain.MocException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.config.SecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La suppression d'un brouillon de demande de changement, à travers la VRAIE
 * chaîne de filtres ({@link SecurityConfig}) — que {@code MocControllerTest} ne
 * charge pas. La règle générique sur les DELETE répondait 403 sans corps au
 * demandeur « user » avant que la règle métier ne soit lue : c'est elle,
 * désormais, qui décide (brouillon, demandeur ou {@code change.analyze}).
 */
@Tag("web")
@WebMvcTest(controllers = {MocController.class, CapaController.class})
@Import(SecurityConfig.class)
class MocDeleteSecurityTest {

    static final String BASE = "/api/v1/changes/";
    static final UUID ID = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean MocService service;
    @MockitoBean CapaService capaService;

    @BeforeEach
    void ctx() { TenantContext.setTenantId(UUID.randomUUID().toString()); }

    @AfterEach
    void clr() { TenantContext.clear(); }

    @Test @WithMockUser(roles = "USER")
    void leDemandeurSupprimeSonBrouillon_204() throws Exception {
        mockMvc.perform(delete(BASE + ID).with(csrf())).andExpect(status().isNoContent());
        verify(service).delete(ID);
    }

    @Test @WithMockUser(roles = "USER")
    void unAutreUtilisateur_403NommeParLeService() throws Exception {
        doThrow(new MocException(MocException.Kind.FORBIDDEN, "moc.not-owner", "x")).when(service).delete(ID);
        mockMvc.perform(delete(BASE + ID).with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("moc.not-owner"));
    }

    @Test @WithMockUser(roles = "USER")
    void uneDemandeQuiNEstPlusUnBrouillon_409() throws Exception {
        doThrow(MocException.conflict("moc.delete-not-draft", "x")).when(service).delete(ID);
        mockMvc.perform(delete(BASE + ID).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("moc.delete-not-draft"));
    }

    @Test @WithMockUser(roles = "AUDITOR")
    void sansChangeRequest_leControleurRefuseToujours() throws Exception {
        mockMvc.perform(delete(BASE + ID).with(csrf())).andExpect(status().isForbidden());
        verify(service, never()).delete(any());
    }

    @Test
    void sansJeton_401() throws Exception {
        mockMvc.perform(delete(BASE + ID).with(csrf())).andExpect(status().isUnauthorized());
        verify(service, never()).delete(any());
    }

    @Test @WithMockUser(roles = "USER")
    void laRegleGeneriqueTientToujoursAilleurs() throws Exception {
        // L'ouverture est limitée à /changes/{id} : la suppression d'une CAPA reste
        // réservée au manager qualité.
        mockMvc.perform(delete("/api/v1/capa/" + UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
        // Une sous-ressource d'une demande ne profite pas de l'ouverture.
        mockMvc.perform(delete(BASE + ID + "/impacts/SAFETY").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
