package com.openlab.qualitos.quality.spc.capability.web;

import com.openlab.qualitos.quality.capa.CapaController;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.config.SecurityConfig;
import com.openlab.qualitos.quality.spc.capability.application.SpcStudyService;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La suppression d'une étude SPC à travers la VRAIE chaîne de filtres
 * ({@link SecurityConfig}), que {@code SpcStudyControllerTest} ne charge pas :
 * c'est l'action {@code spc.manage} qui décide, pas la règle générique des DELETE
 * — sans quoi le directeur qualité, à qui la plateforme accorde l'action, serait
 * refusé avant d'être lu.
 */
@Tag("web")
@WebMvcTest(controllers = {SpcStudyController.class, CapaController.class})
@Import(SecurityConfig.class)
class SpcDeleteSecurityTest {

    static final String BASE = "/api/v1/spc/studies/";
    static final UUID ID = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean SpcStudyService service;
    @MockitoBean CapaService capaService;

    @BeforeEach
    void ctx() { TenantContext.setTenantId(UUID.randomUUID().toString()); }

    @AfterEach
    void clr() { TenantContext.clear(); }

    @Test @WithMockUser(roles = "QUALITY_DIRECTOR")
    void leDirecteurQualiteSupprimeUneEtude_204() throws Exception {
        mockMvc.perform(delete(BASE + ID).with(csrf())).andExpect(status().isNoContent());
        verify(service).delete(ID);
    }

    @Test @WithMockUser(roles = "USER")
    void sansSpcManage_leControleurRefuse() throws Exception {
        mockMvc.perform(delete(BASE + ID).with(csrf())).andExpect(status().isForbidden());
        verify(service, never()).delete(any());
    }

    @Test
    void sansJeton_401() throws Exception {
        mockMvc.perform(delete(BASE + ID).with(csrf())).andExpect(status().isUnauthorized());
        verify(service, never()).delete(any());
    }

    @Test @WithMockUser(roles = "QUALITY_DIRECTOR")
    void laRegleGeneriqueTientToujoursAilleurs() throws Exception {
        // L'ouverture est limitée à /spc/studies/{id} : la suppression d'une CAPA
        // reste réservée au manager qualité, et une sous-ressource n'en profite pas.
        mockMvc.perform(delete("/api/v1/capa/" + UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + ID + "/action-plan").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
