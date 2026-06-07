package com.openlab.qualitos.quality.config;

import com.openlab.qualitos.quality.apikeys.application.ApiKeyService;
import com.openlab.qualitos.quality.apikeys.web.ApiKeyController;
import com.openlab.qualitos.quality.blockchain.application.AnchorVerificationService;
import com.openlab.qualitos.quality.blockchain.application.AnchoringService;
import com.openlab.qualitos.quality.blockchain.web.AnchoringController;
import com.openlab.qualitos.quality.capa.CapaController;
import com.openlab.qualitos.quality.capa.CapaService;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.industry.IndustryPackController;
import com.openlab.qualitos.quality.industry.IndustryPackService;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationService;
import com.openlab.qualitos.quality.tenantmodules.web.ModuleActivationController;
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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * H1 (OWASP A01) — Vérifie les règles d'autorisation par URL de la
 * {@link SecurityConfig} sur les endpoints SENSIBLES : 403 pour un utilisateur non
 * admin, 2xx pour un admin. Les fonctions qualité courantes (NC, CAPA…) restent
 * accessibles aux authentifiés (non régressées : couvertes par leurs tests dédiés).
 *
 * <p>La {@code SecurityConfig} réelle est importée ; {@link JwtDecoder} est mocké
 * (les tests posent le principal via {@code @WithMockUser}, l'OAuth2 resource server
 * n'est donc jamais sollicité).</p>
 */
@Tag("web")
@WebMvcTest(controllers = {
        IndustryPackController.class, ApiKeyController.class, AnchoringController.class,
        ModuleActivationController.class, CapaController.class})
@Import(SecurityConfig.class)
class SecurityConfigAuthorizationTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean IndustryPackService industryPackService;
    @MockitoBean ApiKeyService apiKeyService;
    @MockitoBean AnchoringService anchoringService;
    @MockitoBean AnchorVerificationService anchorVerificationService;
    @MockitoBean ModuleActivationService moduleActivationService;
    @MockitoBean CapaService capaService;

    @BeforeEach
    void ctx() { TenantContext.setTenantId(UUID.randomUUID().toString()); }

    @AfterEach
    void clr() { TenantContext.clear(); }

    // --- Industry pack activate : admin only ---

    @Test @WithMockUser(roles = "USER")
    void packActivate_nonAdmin_403() throws Exception {
        mockMvc.perform(post("/api/v1/industry-packs/manufacturing/activate").with(csrf())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void packActivate_qualityManager_403() throws Exception {
        // QUALITY_MANAGER n'est pas admin : l'activation de pack lui est refusée.
        mockMvc.perform(post("/api/v1/industry-packs/manufacturing/activate").with(csrf())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void packActivate_admin_notForbidden() throws Exception {
        // Admin : autorisé (l'appel atteint le contrôleur ; le service mocké renvoie null,
        // mais on vérifie seulement l'absence de 403).
        mockMvc.perform(post("/api/v1/industry-packs/manufacturing/activate").with(csrf())
                        .contentType("application/json").content("{}"))
                .andExpect(status().is(org.springframework.http.HttpStatus.OK.value()));
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void packActivate_adminTenant_notForbidden() throws Exception {
        // Rôle réel du realm Keycloak (admin_tenant → ROLE_ADMIN_TENANT) : ne doit PAS
        // être verrouillé (régression de lock-out — cf. ADR 0020).
        mockMvc.perform(post("/api/v1/industry-packs/manufacturing/activate").with(csrf())
                        .contentType("application/json").content("{}"))
                .andExpect(status().is(org.springframework.http.HttpStatus.OK.value()));
    }

    @Test @WithMockUser(roles = "USER")
    void packDeactivate_nonAdmin_403() throws Exception {
        mockMvc.perform(delete("/api/v1/industry-packs/manufacturing/activate").with(csrf()))
                .andExpect(status().isForbidden());
    }

    // --- API keys : admin only ---

    @Test @WithMockUser(roles = "USER")
    void apiKeys_nonAdmin_403() throws Exception {
        mockMvc.perform(post("/api/v1/api-keys").with(csrf())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    // --- Blockchain anchor run : admin OR quality manager ---

    @Test @WithMockUser(roles = "USER")
    void anchorRun_plainUser_403() throws Exception {
        mockMvc.perform(post("/api/v1/blockchain/anchor/run").with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void anchorRun_qualityManager_allowed() throws Exception {
        // Action d'intégrité légitime côté pilotage qualité : autorisée (pas de 403).
        mockMvc.perform(post("/api/v1/blockchain/anchor/run").with(csrf()))
                .andExpect(status().is(org.springframework.http.HttpStatus.OK.value()));
    }

    // --- Tenant-modules (administration tenant) : admin only ---

    @Test @WithMockUser(roles = "USER")
    void tenantModuleActivate_plainUser_403() throws Exception {
        mockMvc.perform(post("/api/v1/tenant-modules/activations").with(csrf())
                        .contentType("application/json")
                        .content("{\"moduleCode\":\"kpi\",\"expiresAt\":\"2026-12-31T00:00:00Z\"}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void tenantModuleActivate_qualityManager_403() throws Exception {
        // L'activation d'un module est une action d'administration tenant : refusée au
        // manager qualité (CLAUDE.md §16).
        mockMvc.perform(post("/api/v1/tenant-modules/activations").with(csrf())
                        .contentType("application/json")
                        .content("{\"moduleCode\":\"kpi\",\"expiresAt\":\"2026-12-31T00:00:00Z\"}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN_TENANT")
    void tenantModuleActivate_adminTenant_notForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/tenant-modules/activations").with(csrf())
                        .contentType("application/json")
                        .content("{\"moduleCode\":\"kpi\",\"expiresAt\":\"2026-12-31T00:00:00Z\"}"))
                .andExpect(status().is(org.springframework.http.HttpStatus.CREATED.value()));
    }

    // --- Suppression d'une ressource qualité (DELETE) : manager qualité ou plus ---

    @Test @WithMockUser(roles = "USER")
    void capaDelete_plainUser_403() throws Exception {
        // Un simple "user" ne peut pas supprimer une ressource qualité.
        mockMvc.perform(delete("/api/v1/capa/cases/{id}", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "QUALITY_MANAGER")
    void capaDelete_qualityManager_allowed() throws Exception {
        // Le manager qualité supprime dans son périmètre (pas de 403 ; 204 No Content).
        mockMvc.perform(delete("/api/v1/capa/cases/{id}", UUID.randomUUID()).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test @WithMockUser(roles = "USER")
    void capaCreate_plainUser_allowed() throws Exception {
        // Écriture métier (création CAPA) : RESTE ouverte au user terrain (pas de 403).
        // Le corps invalide → 400, ce qui prouve qu'on a passé la couche d'autorisation.
        mockMvc.perform(post("/api/v1/capa/cases").with(csrf())
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }
}
