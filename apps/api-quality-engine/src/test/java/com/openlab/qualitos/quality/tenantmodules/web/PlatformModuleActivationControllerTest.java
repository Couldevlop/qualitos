package com.openlab.qualitos.quality.tenantmodules.web;

import com.openlab.qualitos.quality.common.MethodSecurityTestConfig;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationDto;
import com.openlab.qualitos.quality.tenantmodules.application.ModuleActivationService;
import com.openlab.qualitos.quality.tenantmodules.domain.ActivationStatus;
import com.openlab.qualitos.quality.tenantmodules.domain.BillingTier;
import com.openlab.qualitos.quality.tenantmodules.domain.ModuleActivationStateException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link MethodSecurityTestConfig} importé : sans lui, la tranche
 * {@code @WebMvcTest} ne charge pas la {@code SecurityConfig} de production et
 * le {@code @PreAuthorize} de classe ne s'applique pas — les bancs de rôle
 * passeraient tous, en ne prouvant rien.
 */
@Tag("web")
@WebMvcTest(controllers = PlatformModuleActivationController.class)
@Import(MethodSecurityTestConfig.class)
class PlatformModuleActivationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ModuleActivationService service;

    static final UUID CLIENT = UUID.randomUUID();
    static final UUID AUTRE = UUID.randomUUID();

    private static String modulesDe(UUID tenant) {
        return "/api/v1/platform/tenants/" + tenant + "/modules";
    }

    private static ModuleActivationDto.ActivationView vue() {
        Instant now = Instant.parse("2026-09-15T10:00:00Z");
        return new ModuleActivationDto.ActivationView(
                UUID.randomUUID(), CLIENT, "controlplan", ActivationStatus.ACTIVE, true,
                BillingTier.STANDARD, null, null, null,
                now, UUID.randomUUID(), now, UUID.randomUUID(), now);
    }

    // ---------- rôle ----------

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void unAdminDeTenantNActivePasPourAutrui() throws Exception {
        // Sans ce refus, un client s'activerait des modules chez un autre — et,
        // ces modules etant factures a l'unite, chez lui-meme sans les payer.
        mockMvc.perform(post(modulesDe(AUTRE) + "/controlplan").with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void unAdminDeTenantNeFermePasPourAutrui() throws Exception {
        mockMvc.perform(delete(modulesDe(AUTRE) + "/controlplan").with(csrf()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(roles = "ADMIN_TENANT")
    void unAdminDeTenantNeLitPasLeCatalogueClientsDeLEditeur() throws Exception {
        // « Quel module pour quel client » est le catalogue commercial de
        // l'editeur : la LECTURE est fermee elle aussi, contrairement a la
        // surface ordinaire ou un admin de tenant doit voir ses propres modules.
        mockMvc.perform(get(modulesDe(AUTRE)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    // ---------- le client vient du CHEMIN ----------

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void leSuperAdminActivePourLeClientDuChemin() throws Exception {
        when(service.activateFor(eq(CLIENT), eq("controlplan"), any())).thenReturn(vue());

        mockMvc.perform(post(modulesDe(CLIENT) + "/controlplan").with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.moduleCode").value("controlplan"))
                .andExpect(jsonPath("$.tenantId").value(CLIENT.toString()));

        verify(service).activateFor(eq(CLIENT), eq("controlplan"), any());
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void aucuneEcheanceTechniqueNEstPoseeALActivation() throws Exception {
        // L'echeance est COMMERCIALE et vit dans l'abonnement d'api-core. En
        // poser une seconde ici creerait deux dates concurrentes pour un seul
        // contrat, et c'est celle que personne ne regarde qui finirait par
        // couper le module.
        when(service.activateFor(eq(CLIENT), eq("controlplan"), eq(null))).thenReturn(vue());

        mockMvc.perform(post(modulesDe(CLIENT) + "/controlplan").with(csrf()))
                .andExpect(status().isCreated());

        verify(service).activateFor(CLIENT, "controlplan", null);
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void leSuperAdminFermePourLeClientDuChemin() throws Exception {
        mockMvc.perform(delete(modulesDe(CLIENT) + "/controlplan").with(csrf()))
                .andExpect(status().isNoContent());

        verify(service).deactivateFor(CLIENT, "controlplan");
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void leSuperAdminListeLesModulesDUnClient() throws Exception {
        when(service.listFor(CLIENT)).thenReturn(List.of(vue()));

        mockMvc.perform(get(modulesDe(CLIENT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].moduleCode").value("controlplan"));
    }

    // ---------- refus du domaine ----------

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    void fermerUnModuleDuSocleEstRefuse() throws Exception {
        // Le socle n'est pas facture a l'unite : le fermer priverait le client
        // d'ecriture sur ce qu'il a de droit.
        doThrow(new ModuleActivationStateException("Cannot disable a core module: pdca"))
                .when(service).deactivateFor(CLIENT, "pdca");

        mockMvc.perform(delete(modulesDe(CLIENT) + "/pdca").with(csrf()))
                .andExpect(status().isConflict());
    }
}
