package com.openlab.qualitos.quality.dmaic.resolution.web;

import com.openlab.qualitos.quality.capa.CapaController;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.config.SecurityConfig;
import com.openlab.qualitos.quality.dmaic.resolution.application.ResolutionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Retirer une photo de la fiche à travers la VRAIE chaîne de filtres
 * ({@link SecurityConfig}) : c'est l'action {@code dmaic.manage} qui décide,
 * pas la règle générique des DELETE, et l'ouverture ne vaut que pour un
 * emplacement de photo.
 */
@Tag("web")
@WebMvcTest(controllers = {ResolutionController.class, CapaController.class})
@Import(SecurityConfig.class)
class ResolutionDeleteSecurityTest {

    static final UUID PROJET = UUID.randomUUID();
    static final String PHOTO = "/api/v1/dmaic/projects/" + PROJET + "/resolution/photos/3";

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean ResolutionService service;
    @MockitoBean CapaService capaService;

    @BeforeEach
    void ctx() { TenantContext.setTenantId(UUID.randomUUID().toString()); }

    @AfterEach
    void clr() { TenantContext.clear(); }

    @Test @WithMockUser(roles = "QUALITY_DIRECTOR")
    void leDirecteurQualiteRetireUnePhoto_204() throws Exception {
        mockMvc.perform(delete(PHOTO).with(csrf())).andExpect(status().isNoContent());
        verify(service).deletePhoto(PROJET, 3);
    }

    @Test @WithMockUser(roles = "USER")
    void sansDmaicManage_leControleurRefuse() throws Exception {
        mockMvc.perform(delete(PHOTO).with(csrf())).andExpect(status().isForbidden());
        verify(service, never()).deletePhoto(any(), anyInt());
    }

    @Test
    void sansJeton_401() throws Exception {
        mockMvc.perform(delete(PHOTO).with(csrf())).andExpect(status().isUnauthorized());
        verify(service, never()).deletePhoto(any(), anyInt());
    }

    @Test @WithMockUser(roles = "QUALITY_DIRECTOR")
    void laRegleGeneriqueTientToujoursAilleurs() throws Exception {
        mockMvc.perform(delete("/api/v1/capa/" + UUID.randomUUID()).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/dmaic/projects/" + PROJET + "/resolution").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
