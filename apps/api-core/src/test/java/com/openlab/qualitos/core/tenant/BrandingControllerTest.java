package com.openlab.qualitos.core.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.core.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La marque du client, vue par le client lui-même.
 *
 * <p>Cette route est séparée de celle de l'éditeur pour deux raisons, et la
 * seconde est celle qui compte :
 *
 * <ul>
 *   <li>{@code /api/v1/tenants/**} est réservé au super administrateur : un
 *       administrateur de client n'y entre pas, alors que son identité le
 *       regarde au premier chef ({@code feedback_parametrage_admin_tenant}) ;</li>
 *   <li>{@code /api/v1/tenants/**} porte {@code @SaasOnly} : il répond 404 dans
 *       une installation on-premise (ADR 0082). Y laisser l'identité aurait
 *       interdit à tout client on-premise de poser son logo sur sa propre
 *       installation — exactement ce qu'il attend le plus.</li>
 * </ul>
 *
 * <p>Le client cible n'est jamais lu du corps ni du chemin : il vient du jeton
 * ({@code CLAUDE.md} §18.2.2).
 */
@WebMvcTest(BrandingController.class)
@Import(com.openlab.qualitos.core.config.SecurityConfig.class)
class BrandingControllerTest {

    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TenantIdentityService identityService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private static TenantIdentityDto.Response identity() {
        return new TenantIdentityDto.Response(
                "La qualité, mesurée", "Clinique du Parc SA", "SA", "552100554", null,
                null, null, null, "Montpellier", "FR", null, null,
                "#0B6B5C", "#1D5FA8", TenantTheme.DENSE_LIGHT, true);
    }

    private static TenantIdentityDto.Request request() {
        return new TenantIdentityDto.Request(
                "La qualité, mesurée", "Clinique du Parc SA", "SA", "552100554", null,
                null, null, null, "Montpellier", "FR", null, null,
                "#0B6B5C", "#1D5FA8", "DENSE_LIGHT");
    }

    @Nested
    @DisplayName("GET /api/v1/branding")
    class ReadBranding {

        @Test
        @WithMockUser(roles = "USER")
        void toutMembreAuthentifieLitLaMarqueDeSonClient() throws Exception {
            TenantContext.setTenantId(TENANT.toString());
            when(identityService.get(TENANT)).thenReturn(identity());

            mockMvc.perform(get("/api/v1/branding"))
                    .andExpect(status().isOk())
                    // L'application a besoin du thème et des couleurs pour se rendre :
                    // les refuser à un utilisateur ordinaire rendrait l'écran nu.
                    .andExpect(jsonPath("$.themeCode").value("DENSE_LIGHT"))
                    .andExpect(jsonPath("$.primaryColor").value("#0B6B5C"))
                    .andExpect(jsonPath("$.slogan").value("La qualité, mesurée"));
        }

        @Test
        @WithMockUser(roles = "USER")
        void sansClientDansLeJetonLaDemandeEstRefusee() throws Exception {
            // 401, comme partout ailleurs dans api-core : un jeton sans client
            // n'est pas une requete mal formee, c'est une requete non situee
            // (MissingTenantContextException).
            mockMvc.perform(get("/api/v1/branding")).andExpect(status().isUnauthorized());
            verify(identityService, never()).get(any());
        }

        @Test
        void sansAuthentificationLaMarqueNEstPasServie() throws Exception {
            // Le logo et le slogan d'un client ne sont pas publics : ils nomment
            // ses sites, sa raison sociale, son numéro de TVA.
            mockMvc.perform(get("/api/v1/branding")).andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/branding")
    class WriteBranding {

        @Test
        @WithMockUser(roles = "ADMIN_TENANT")
        void lAdministrateurDuClientPoseSaMarque() throws Exception {
            TenantContext.setTenantId(TENANT.toString());
            when(identityService.save(eq(TENANT), any())).thenReturn(identity());

            mockMvc.perform(put("/api/v1/branding").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.legalName").value("Clinique du Parc SA"));
        }

        @Test
        @WithMockUser(roles = "USER")
        void unMembreOrdinaireNeChangePasLaMarque() throws Exception {
            TenantContext.setTenantId(TENANT.toString());

            mockMvc.perform(put("/api/v1/branding").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isForbidden());
            verify(identityService, never()).save(any(), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN_TENANT")
        void uneCouleurIllisibleEstRefuseeAvecSaRaison() throws Exception {
            TenantContext.setTenantId(TENANT.toString());
            when(identityService.save(eq(TENANT), any()))
                    .thenThrow(new IllegalArgumentException("contraste insuffisant entre #EEEEEE et #FFFFFF"));

            mockMvc.perform(put("/api/v1/branding").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        void lEditeurPeutDepannerLaMarqueDUnClient() throws Exception {
            // Depanner, pas gouverner : il ecrit la marque du client dont le
            // jeton porte l'identifiant, jamais celle d'un autre.
            TenantContext.setTenantId(TENANT.toString());
            when(identityService.save(eq(TENANT), any())).thenReturn(identity());

            mockMvc.perform(put("/api/v1/branding").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN_TENANT")
        void sansClientDansLeJetonLEcritureEstRefusee() throws Exception {
            mockMvc.perform(put("/api/v1/branding").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request())))
                    .andExpect(status().isUnauthorized());
            verify(identityService, never()).save(any(), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN_TENANT")
        void uneCouleurMalFormeeEstRefuseeParLaValidation() throws Exception {
            TenantContext.setTenantId(TENANT.toString());
            String corps = objectMapper.writeValueAsString(new TenantIdentityDto.Request(
                    null, "Clinique", null, null, null, null, null, null, null, null, null, null,
                    "vert", null, "DENSE_LIGHT"));

            mockMvc.perform(put("/api/v1/branding").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(corps))
                    .andExpect(status().isBadRequest());
            verify(identityService, never()).save(any(), any());
        }
    }
}
