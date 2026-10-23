package com.openlab.qualitos.quality.training.competencymatrix.web;

import com.openlab.qualitos.quality.capa.CapaController;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.config.SecurityConfig;
import com.openlab.qualitos.quality.training.competencymatrix.application.CompetencyMatrixService;
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
 * Retirer un opérateur ou une compétence à travers la VRAIE chaîne de filtres
 * ({@link SecurityConfig}) : c'est l'action {@code training.manage} qui décide,
 * pas la règle générique des DELETE — sans quoi le directeur qualité, à qui la
 * plateforme accorde l'action, serait refusé avant d'être lu.
 */
@Tag("web")
@WebMvcTest(controllers = {CompetencyMatrixController.class, CapaController.class})
@Import(SecurityConfig.class)
class CompetencyMatrixDeleteSecurityTest {

    static final String BASE = "/api/v1/training/competencies/matrix/";
    static final UUID ID = UUID.randomUUID();

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean CompetencyMatrixService service;
    @MockitoBean CapaService capaService;

    @BeforeEach
    void ctx() { TenantContext.setTenantId(UUID.randomUUID().toString()); }

    @AfterEach
    void clr() { TenantContext.clear(); }

    @Test @WithMockUser(roles = "QUALITY_DIRECTOR")
    void leDirecteurQualiteRetireUnOperateurEtUneCompetence() throws Exception {
        mockMvc.perform(delete(BASE + "operators/" + ID).with(csrf())).andExpect(status().isOk());
        mockMvc.perform(delete(BASE + "skills/" + ID).with(csrf())).andExpect(status().isOk());
        verify(service).removeOperator(ID);
        verify(service).removeSkill(ID);
    }

    @Test @WithMockUser(roles = "USER")
    void sansTrainingManage_leControleurRefuse() throws Exception {
        mockMvc.perform(delete(BASE + "operators/" + ID).with(csrf())).andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "skills/" + ID).with(csrf())).andExpect(status().isForbidden());
        verify(service, never()).removeOperator(any());
        verify(service, never()).removeSkill(any());
    }

    @Test
    void sansJeton_401() throws Exception {
        mockMvc.perform(delete(BASE + "operators/" + ID).with(csrf())).andExpect(status().isUnauthorized());
        verify(service, never()).removeOperator(any());
    }

    @Test @WithMockUser(roles = "QUALITY_DIRECTOR")
    void laRegleGeneriqueTientToujoursAilleurs() throws Exception {
        mockMvc.perform(delete("/api/v1/capa/" + UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "operators/" + ID + "/autre").with(csrf()))
                .andExpect(status().isForbidden());
    }
}
