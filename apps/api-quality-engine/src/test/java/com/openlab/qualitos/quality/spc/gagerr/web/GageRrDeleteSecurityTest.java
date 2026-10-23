package com.openlab.qualitos.quality.spc.gagerr.web;

import com.openlab.qualitos.quality.capa.CapaController;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.config.SecurityConfig;
import com.openlab.qualitos.quality.spc.gagerr.application.GageRrStudyService;
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
 * La suppression d'une étude R&amp;R à travers la VRAIE chaîne de filtres
 * ({@link SecurityConfig}) : c'est l'action {@code spc.manage} qui décide, pas
 * la règle générique des DELETE, et l'ouverture ne vaut que pour
 * {@code /spc/gage-rr/{id}}.
 */
@Tag("web")
@WebMvcTest(controllers = {GageRrStudyController.class, CapaController.class})
@Import(SecurityConfig.class)
class GageRrDeleteSecurityTest {

    static final String BASE = "/api/v1/spc/gage-rr/";
    static final UUID ID = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean GageRrStudyService service;
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
        mockMvc.perform(delete("/api/v1/capa/" + UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + ID + "/mesures").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
