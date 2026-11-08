package com.openlab.qualitos.core.platformaudit;

import com.openlab.qualitos.core.common.GlobalExceptionHandler;
import com.openlab.qualitos.core.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La surface de la piste d'audit de plateforme.
 *
 * <p>Comme {@code ModulePriceControllerTest}, la VRAIE {@link SecurityConfig}
 * du module est montée : c'est elle qui porte la règle de chemin, et un banc
 * qui la remplacerait par une configuration de test vérifierait une sécurité
 * qui n'est pas celle de la production.
 *
 * <p>Le banc le plus important est
 * {@link Portee#aucune_ecriture_n_est_exposee} : il fixe par un test ce que la
 * classe affirme en commentaire.
 */
@WebMvcTest(PlatformAuditController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("PlatformAuditController")
class PlatformAuditControllerTest {

    private static final UUID EDITEUR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID CLIENT = UUID.fromString("00000000-0000-0000-0000-0000000000c1");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlatformAuditService service;

    /** SecurityConfig déclare un TenantJwtFilter(JwtDecoder) : sans ce mock, pas de contexte. */
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Nested
    @DisplayName("portée")
    class Portee {

        @Test
        @DisplayName("le super-administrateur lit le journal")
        void editeur_autorise() throws Exception {
            when(service.list(any(), any(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of(acte())));

            mockMvc.perform(get("/api/v1/platform-audit").with(editeur()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].action").value("TENANT_CREATED"))
                    // L'empreinte est rendue : sans elle, le destinataire d'un
                    // extrait devrait nous croire sur parole.
                    .andExpect(jsonPath("$.content[0].integrityHash").exists());
        }

        @Test
        @DisplayName("un administrateur de client ne lit pas le journal de l'editeur")
        void admin_client_refuse() throws Exception {
            mockMvc.perform(get("/api/v1/platform-audit").with(adminClient()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("sans jeton, rien")
        void anonyme_refuse() throws Exception {
            mockMvc.perform(get("/api/v1/platform-audit"))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(service);
        }

        @Test
        @DisplayName("aucune ecriture n'est exposee")
        void aucune_ecriture_n_est_exposee() throws Exception {
            // Une surface d'écriture, même réservée à l'éditeur, permettrait de
            // FABRIQUER un acte qui n'a pas eu lieu — exactement ce qu'un
            // auditeur cherche à exclure. Le journal du moteur de qualité, lui,
            // porte un POST ; celui-ci ne doit jamais en acquérir un.
            mockMvc.perform(post("/api/v1/platform-audit")
                            .with(editeur())
                            .with(org.springframework.security.test.web.servlet.request
                                    .SecurityMockMvcRequestPostProcessors.csrf())
                            .contentType("application/json")
                            .content("{\"action\":\"TENANT_CREATED\"}"))
                    .andExpect(status().isMethodNotAllowed());

            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("filtres")
    class Filtres {

        @Test
        @DisplayName("tout ce qui a touche un client")
        void par_client() throws Exception {
            when(service.list(any(), any(), any(), any(), any(), any()))
                    .thenReturn(new PageImpl<>(List.of()));

            mockMvc.perform(get("/api/v1/platform-audit")
                            .param("targetTenantId", CLIENT.toString())
                            .with(editeur()))
                    .andExpect(status().isOk());

            verify(service).list(eq(null), eq(CLIENT), eq(null), eq(null), eq(null), any());
        }

        @Test
        @DisplayName("un acte inconnu de l'enumeration est refuse en 400, pas en 500")
        void acte_inconnu() throws Exception {
            mockMvc.perform(get("/api/v1/platform-audit")
                            .param("action", "TENANT_SUPPRIME")
                            .with(editeur()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("vérification")
    class Verification {

        @Test
        @DisplayName("sans bornes, la fenetre couvre TOUT le journal")
        void fenetre_par_defaut() throws Exception {
            when(service.derniereSequence()).thenReturn(120L);
            when(service.verifyChain(1L, 120L)).thenReturn(
                    new PlatformAuditDto.Verification(1L, 120L, 120, true, List.of()));

            mockMvc.perform(get("/api/v1/platform-audit/verify").with(editeur()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.intact").value(true));

            // Une valeur par défaut étroite rendrait un « intact » rassurant
            // sur trois lignes.
            verify(service).verifyChain(1L, 120L);
        }

        @Test
        @DisplayName("journal vide : la fenetre ne devient pas absurde")
        void journal_vide() throws Exception {
            when(service.derniereSequence()).thenReturn(0L);
            when(service.verifyChain(1L, 1L)).thenReturn(
                    new PlatformAuditDto.Verification(1L, 1L, 0, false, List.of()));

            mockMvc.perform(get("/api/v1/platform-audit/verify").with(editeur()))
                    .andExpect(status().isOk());

            // Sans le Math.max, la borne haute tomberait à 0 et le service
            // refuserait sa propre valeur par défaut.
            verify(service).verifyChain(1L, 1L);
        }

        @Test
        @DisplayName("les bornes demandees sont respectees")
        void fenetre_demandee() throws Exception {
            when(service.verifyChain(10L, 20L)).thenReturn(
                    new PlatformAuditDto.Verification(10L, 20L, 11, true, List.of()));

            mockMvc.perform(get("/api/v1/platform-audit/verify")
                            .param("fromSeq", "10").param("toSeq", "20")
                            .with(editeur()))
                    .andExpect(status().isOk());

            verify(service).verifyChain(10L, 20L);
        }

        @Test
        @DisplayName("une fenetre refusee par le service rend 400")
        void fenetre_refusee() throws Exception {
            when(service.verifyChain(5L, 2L))
                    .thenThrow(new PlatformAuditStateException("Fenetre vide"));

            mockMvc.perform(get("/api/v1/platform-audit/verify")
                            .param("fromSeq", "5").param("toSeq", "2")
                            .with(editeur()))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("export")
    class Export {

        @Test
        @DisplayName("servi en piece jointe, le sceau dans le nom, sans cache")
        void piece_jointe() throws Exception {
            String sceau = "abcdef0123456789".repeat(4);
            when(service.derniereSequence()).thenReturn(3L);
            when(service.export(1L, 3L)).thenReturn(new PlatformAuditDto.Export(
                    Instant.parse("2026-11-08T09:30:00Z"), 1L, 3L, 3, sceau,
                    new PlatformAuditDto.Verification(1L, 3L, 3, true, List.of()),
                    List.of(acte())));

            mockMvc.perform(get("/api/v1/platform-audit/export").with(editeur()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("application/json"))
                    // Deux dossiers du même intervalle extraits à deux dates ne
                    // doivent pas se confondre sur un bureau.
                    .andExpect(header().string("Content-Disposition",
                            "attachment; filename=\"qualitos-piste-audit-1-3-abcdef012345.json\""))
                    .andExpect(header().string("Cache-Control", "no-store"))
                    .andExpect(jsonPath("$.seal").value(sceau))
                    .andExpect(jsonPath("$.verification.intact").value(true));
        }

        @Test
        @DisplayName("un client ne telecharge pas le dossier de l'editeur")
        void export_refuse() throws Exception {
            mockMvc.perform(get("/api/v1/platform-audit/export").with(adminClient()))
                    .andExpect(status().isForbidden());

            verifyNoInteractions(service);
        }
    }

    // ------------------------------------------------------------- plomberie

    private static PlatformAuditDto.Event acte() {
        return new PlatformAuditDto.Event(
                UUID.randomUUID(), 1L,
                Instant.parse("2026-11-08T09:30:00Z"), Instant.parse("2026-11-08T09:30:00Z"),
                PlatformActorType.USER, EDITEUR, PlatformAuditAction.TENANT_CREATED,
                "tenant", CLIENT, CLIENT, "Client cree : Metalux", null,
                "203.0.113.7", "Chrome/130", "a".repeat(64), null);
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor editeur() {
        return jwt().jwt(builder -> builder.subject(EDITEUR.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor adminClient() {
        return jwt().jwt(builder -> builder.subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN_TENANT"));
    }
}
