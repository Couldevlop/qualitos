package com.openlab.qualitos.quality.config;

import com.openlab.qualitos.quality.apikeys.application.ApiKeyService;
import com.openlab.qualitos.quality.apikeys.web.ApiKeyController;
import com.openlab.qualitos.quality.blockchain.application.AnchorVerificationService;
import com.openlab.qualitos.quality.blockchain.application.AnchoringService;
import com.openlab.qualitos.quality.blockchain.web.AnchoringController;
import com.openlab.qualitos.quality.common.TenantContext;
import com.openlab.qualitos.quality.industry.IndustryPackController;
import com.openlab.qualitos.quality.industry.IndustryPackService;
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
        IndustryPackController.class, ApiKeyController.class, AnchoringController.class})
@Import(SecurityConfig.class)
class SecurityConfigAuthorizationTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean IndustryPackService industryPackService;
    @MockitoBean ApiKeyService apiKeyService;
    @MockitoBean AnchoringService anchoringService;
    @MockitoBean AnchorVerificationService anchorVerificationService;

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
}
