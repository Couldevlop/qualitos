package com.openlab.qualitos.core.tenant;

import com.openlab.qualitos.core.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le logo du client : le poser, le servir, le retirer.
 *
 * <p>C'est le seul fichier qu'un client televerse et que la plateforme sert
 * ensuite depuis SON domaine, a tous ses membres, sur chaque ecran. Les
 * verifications d'en-tetes plus bas ne sont donc pas du zele : un fichier
 * servi sans elles s'affiche dans le contexte de l'application.
 *
 * <p>Comme pour {@link BrandingControllerTest}, la route vit hors de
 * {@code /api/v1/tenants/**} : celui-ci porte {@code @SaasOnly} et repondrait
 * 404 en on-premise, la ou poser sa marque sur son installation importe le plus.
 */
@WebMvcTest(TenantLogoController.class)
@Import(com.openlab.qualitos.core.config.SecurityConfig.class)
class TenantLogoControllerTest {

    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final String SHA = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TenantIdentityService identityService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    private static byte[] png() {
        byte[] octets = new byte[64];
        byte[] signature = {(byte) 0x89, 80, 78, 71, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, octets, 0, signature.length);
        return octets;
    }

    private static TenantLogo logo() {
        TenantLogo logo = new TenantLogo();
        logo.setTenantId(TENANT);
        logo.setContentType("image/png");
        logo.setSizeBytes(64);
        logo.setSha256(SHA);
        logo.setContent(png());
        return logo;
    }

    private static MockMultipartFile fichier(String contentType) {
        return new MockMultipartFile("file", "ma-marque.png", contentType, png());
    }

    @Nested
    @DisplayName("GET /api/v1/branding/logo")
    class ServirLeLogo {

        @Test
        @WithMockUser(roles = "USER")
        void toutMembreVoitLeLogoDeSonClient() throws Exception {
            TenantContext.setTenantId(TENANT.toString());
            when(identityService.logo(TENANT)).thenReturn(Optional.of(logo()));

            mockMvc.perform(get("/api/v1/branding/logo"))
                    .andExpect(status().isOk())
                    // Le type servi est celui qu'a VALIDE la politique, jamais
                    // celui qu'annoncait le client.
                    .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                    // Le navigateur ne doit pas redeviner le type : un fichier
                    // devine autrement, c'est un fichier execute autrement.
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    // Ceinture : meme si un contenu actif passait les controles,
                    // il n'aurait aucune permission.
                    .andExpect(header().string("Content-Security-Policy",
                            "default-src 'none'; sandbox"))
                    // Nom de fichier fixe : celui d'origine est une chaine venue
                    // du client et ne sert a rien ici.
                    .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                            org.hamcrest.Matchers.containsString("inline")))
                    .andExpect(header().string(HttpHeaders.ETAG, "\"" + SHA + "\""));
        }

        @Test
        @WithMockUser(roles = "USER")
        void unLogoInchangeNEstPasRenvoye() throws Exception {
            // L'ETag est l'empreinte du contenu : le navigateur revient avec
            // elle, et le serveur repond 304 sans reecrire 512 Kio.
            TenantContext.setTenantId(TENANT.toString());
            when(identityService.logo(TENANT)).thenReturn(Optional.of(logo()));

            mockMvc.perform(get("/api/v1/branding/logo")
                            .header(HttpHeaders.IF_NONE_MATCH, "\"" + SHA + "\""))
                    .andExpect(status().isNotModified());
        }

        @Test
        @WithMockUser(roles = "USER")
        void unLogoModifieEstBienRenvoye() throws Exception {
            TenantContext.setTenantId(TENANT.toString());
            when(identityService.logo(TENANT)).thenReturn(Optional.of(logo()));

            mockMvc.perform(get("/api/v1/branding/logo")
                            .header(HttpHeaders.IF_NONE_MATCH, "\"empreinte-perimee\""))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "USER")
        void sansLogoLaRouteRepondIntrouvable() throws Exception {
            TenantContext.setTenantId(TENANT.toString());
            when(identityService.logo(TENANT)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/branding/logo")).andExpect(status().isNotFound());
        }

        @Test
        void sansAuthentificationLeLogoNEstPasServi() throws Exception {
            mockMvc.perform(get("/api/v1/branding/logo")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser(roles = "USER")
        void sansClientDansLeJetonLaDemandeEstRefusee() throws Exception {
            mockMvc.perform(get("/api/v1/branding/logo")).andExpect(status().isUnauthorized());
            verify(identityService, never()).logo(any());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/branding/logo")
    class PoserLeLogo {

        @Test
        @WithMockUser(roles = "ADMIN_TENANT")
        void lAdministrateurDuClientPoseSonLogo() throws Exception {
            TenantContext.setTenantId(TENANT.toString());
            when(identityService.saveLogo(eq(TENANT), eq("image/png"), any())).thenReturn(SHA);

            mockMvc.perform(multipart("/api/v1/branding/logo").file(fichier("image/png")).with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(roles = "USER")
        void unMembreOrdinaireNePosePasDeLogo() throws Exception {
            TenantContext.setTenantId(TENANT.toString());

            mockMvc.perform(multipart("/api/v1/branding/logo").file(fichier("image/png")).with(csrf()))
                    .andExpect(status().isForbidden());
            verify(identityService, never()).saveLogo(any(), any(), any());
        }

        @Test
        @WithMockUser(roles = "ADMIN_TENANT")
        void unSvgEstRefuseAvecUnStatutDeTypeNonSupporte() throws Exception {
            // 415, pas 400 : le geste attendu est de convertir le fichier, et
            // un client programmatique le lit dans le statut, pas le message.
            TenantContext.setTenantId(TENANT.toString());
            when(identityService.saveLogo(eq(TENANT), any(), any()))
                    .thenThrow(new LogoRejectedException(LogoRejectedException.Reason.TYPE,
                            "type de fichier refuse : image/svg+xml"));

            mockMvc.perform(multipart("/api/v1/branding/logo")
                            .file(fichier("image/svg+xml")).with(csrf()))
                    .andExpect(status().isUnsupportedMediaType())
                    .andExpect(jsonPath("$.reason").value("TYPE"));
        }

        @Test
        @WithMockUser(roles = "ADMIN_TENANT")
        void unFichierTropLourdEstRefuseEnChargeTropGrande() throws Exception {
            TenantContext.setTenantId(TENANT.toString());
            when(identityService.saveLogo(eq(TENANT), any(), any()))
                    .thenThrow(new LogoRejectedException(LogoRejectedException.Reason.SIZE,
                            "fichier trop volumineux : 8000000 octets, limite 512 Kio"));

            mockMvc.perform(multipart("/api/v1/branding/logo")
                            .file(fichier("image/png")).with(csrf()))
                    .andExpect(status().isPayloadTooLarge())
                    .andExpect(jsonPath("$.reason").value("SIZE"));
        }

        @Test
        @WithMockUser(roles = "ADMIN_TENANT")
        void unTypeDementiParLeContenuEstRefuseEnRequeteInvalide() throws Exception {
            TenantContext.setTenantId(TENANT.toString());
            when(identityService.saveLogo(eq(TENANT), any(), any()))
                    .thenThrow(new LogoRejectedException(LogoRejectedException.Reason.CONTENT,
                            "le contenu du fichier ne correspond pas au type annonce (image/png)"));

            mockMvc.perform(multipart("/api/v1/branding/logo")
                            .file(fichier("image/png")).with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.reason").value("CONTENT"));
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        void lEditeurPeutDepannerLeLogoDUnClient() throws Exception {
            TenantContext.setTenantId(TENANT.toString());
            when(identityService.saveLogo(eq(TENANT), any(), any())).thenReturn(SHA);

            mockMvc.perform(multipart("/api/v1/branding/logo").file(fichier("image/png")).with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(roles = "ADMIN_TENANT")
        void sansClientDansLeJetonLeTeleversementEstRefuse() throws Exception {
            mockMvc.perform(multipart("/api/v1/branding/logo").file(fichier("image/png")).with(csrf()))
                    .andExpect(status().isUnauthorized());
            verify(identityService, never()).saveLogo(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/branding/logo")
    class RetirerLeLogo {

        @Test
        @WithMockUser(roles = "ADMIN_TENANT")
        void lAdministrateurRetireLeLogo() throws Exception {
            TenantContext.setTenantId(TENANT.toString());

            mockMvc.perform(delete("/api/v1/branding/logo").with(csrf()))
                    .andExpect(status().isNoContent());
            verify(identityService).deleteLogo(TENANT);
        }

        @Test
        @WithMockUser(roles = "USER")
        void unMembreOrdinaireNeRetirePasLeLogo() throws Exception {
            TenantContext.setTenantId(TENANT.toString());

            mockMvc.perform(delete("/api/v1/branding/logo").with(csrf()))
                    .andExpect(status().isForbidden());
            verify(identityService, never()).deleteLogo(any());
        }
    }
}
