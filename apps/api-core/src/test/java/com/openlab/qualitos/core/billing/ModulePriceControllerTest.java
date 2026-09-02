package com.openlab.qualitos.core.billing;

import com.openlab.qualitos.core.common.GlobalExceptionHandler;
import com.openlab.qualitos.core.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Même convention que {@code BillingProfileControllerTest} : la vraie
 * {@link SecurityConfig} de ce module est montée (pas de
 * {@code MethodSecurityTestConfig}, qui vit dans le moteur de qualité), pour
 * exercer la même chaîne de filtres qu'en production, {@code @PreAuthorize}
 * compris.
 *
 * <p>{@code jwt()} plutôt que {@code @WithMockUser} — comme
 * {@code TenantControllerTest} — parce que {@link ModulePriceController}
 * résout l'acteur via {@code CurrentUser.requireUserId()}, qui pour un
 * {@code JwtAuthenticationToken} lit le claim {@code sub} du jeton : il faut
 * un vrai principal JWT dont on contrôle le sujet pour vérifier que l'acteur
 * transmis au service est bien celui-là — et, ci-dessous, pour vérifier
 * qu'un sujet non-UUID produit un refus propre (401) plutôt qu'un 500.
 */
@WebMvcTest(ModulePriceController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("ModulePriceController")
class ModulePriceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ModulePriceService service;

    // SecurityConfig déclare un bean TenantJwtFilter(JwtDecoder) : sans ce
    // mock, le contexte @WebMvcTest ne démarre pas — même motif que dans
    // BillingProfileControllerTest.
    @MockBean
    private JwtDecoder jwtDecoder;

    private static final UUID ACTOR = UUID.randomUUID();

    private static final String CORPS_VALIDE = """
            {
              "moduleCode": "controlplan",
              "billingTier": "STANDARD",
              "period": "MONTHLY",
              "amountCents": 9900,
              "currency": "EUR"
            }
            """;

    private static final String CORPS_MONTANT_NEGATIF = """
            {
              "moduleCode": "controlplan",
              "billingTier": "STANDARD",
              "period": "MONTHLY",
              "amountCents": -1,
              "currency": "EUR"
            }
            """;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor superAdminJwtDe(UUID sujet) {
        return jwt().jwt(builder -> builder.subject(sujet.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminTenantJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN_TENANT"));
    }

    // Un sub qui n'est pas un UUID : jeton de compte de service, ou tout
    // principal dont l'annuaire ne pose pas un UUID en identifiant.
    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor superAdminJwtAvecSubNonUuid() {
        return jwt().jwt(builder -> builder.subject("service-account-facturation"))
                .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
    }

    private ModulePriceDto.View vueAttendue() {
        Instant now = Instant.now();
        return new ModulePriceDto.View(
                UUID.randomUUID(), "controlplan", BillingTier.STANDARD, BillingPeriod.MONTHLY,
                9900, "EUR", ACTOR, now);
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/module-prices")
    class SetPrice {

        @Test
        void unAdminDeTenantNeFixePasLeTarifDesModules() throws Exception {
            // La facturation est une affaire d'EDITEUR. Un administrateur de tenant
            // qui pourrait fixer ses propres tarifs pourrait se les baisser lui-meme.
            mockMvc.perform(put("/api/v1/admin/module-prices")
                            .with(adminTenantJwt()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CORPS_VALIDE))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }

        @Test
        void leSuperAdminFixeLeTarif_avecLActeurTireDuJeton() throws Exception {
            when(service.setPrice(any(), eq(ACTOR))).thenReturn(vueAttendue());

            mockMvc.perform(put("/api/v1/admin/module-prices")
                            .with(superAdminJwtDe(ACTOR)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CORPS_VALIDE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.moduleCode").value("controlplan"))
                    .andExpect(jsonPath("$.updatedBy").value(ACTOR.toString()));

            verify(service).setPrice(any(), eq(ACTOR));
        }

        @Test
        void unMontantNegatifEstRefuseAvantDAtteindreLeService() throws Exception {
            // chk_price_amount en base ; ici, on veut le refus avant l'ecriture, pas
            // une exception SQL generique en retour (meme motif que BillingProfile).
            mockMvc.perform(put("/api/v1/admin/module-prices")
                            .with(superAdminJwtDe(ACTOR)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CORPS_MONTANT_NEGATIF))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }

        @Test
        void unSubNonUuidEstRefuseProprement_pasUn500() throws Exception {
            // Le defaut corrige par cette ronde : sans CurrentUser, UUID.fromString()
            // levait une IllegalArgumentException non geree, qui tombait dans le
            // catch-all du GlobalExceptionHandler et rendait un 500 generique sur
            // une action d'administration de facturation.
            mockMvc.perform(put("/api/v1/admin/module-prices")
                            .with(superAdminJwtAvecSubNonUuid()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CORPS_VALIDE))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/module-prices")
    class ListPrices {

        @Test
        void unAdminDeTenantNeConsultePasLeCatalogueDeTarifs() throws Exception {
            mockMvc.perform(get("/api/v1/admin/module-prices").with(adminTenantJwt()))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }

        @Test
        void leSuperAdminConsulteLeCatalogueComplet() throws Exception {
            when(service.findAll()).thenReturn(List.of(vueAttendue()));

            mockMvc.perform(get("/api/v1/admin/module-prices").with(superAdminJwtDe(ACTOR)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].moduleCode").value("controlplan"));
        }

        @Test
        void leCatalogueVideRenvoieUneListeVide_pasUneErreur() throws Exception {
            when(service.findAll()).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/admin/module-prices").with(superAdminJwtDe(ACTOR)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }
}
