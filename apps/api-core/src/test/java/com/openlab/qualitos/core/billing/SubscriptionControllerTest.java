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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Même convention que {@code ModulePriceControllerTest} : la vraie
 * {@link SecurityConfig} est montée, et l'authentification passe par
 * {@code jwt()} — le contrôleur résout l'acteur via
 * {@code CurrentUser.requireUserId()}, il faut donc un principal JWT dont on
 * contrôle le sujet.
 */
@WebMvcTest(SubscriptionController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("SubscriptionController")
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SubscriptionService service;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID CLIENT = UUID.randomUUID();
    private static final UUID ABONNEMENT = UUID.randomUUID();

    private static final String BASE = "/api/v1/admin/clients/" + CLIENT + "/subscriptions";

    private static final String CORPS_VALIDE = """
            {
              "moduleCode": "controlplan",
              "billingTier": "STANDARD",
              "period": "MONTHLY"
            }
            """;

    // Le corps porte un tenantId : Jackson l'ignore, la commande n'ayant pas ce
    // champ. C'est exactement la garantie qu'on veut vérifier (§18.2 règle 2).
    private static final String CORPS_AVEC_TENANT_FORGE = """
            {
              "tenantId": "00000000-0000-0000-0000-000000000666",
              "moduleCode": "controlplan",
              "billingTier": "STANDARD",
              "period": "MONTHLY"
            }
            """;

    private static final String CORPS_SANS_MODULE = """
            {
              "billingTier": "STANDARD",
              "period": "MONTHLY"
            }
            """;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor superAdminJwtDe(UUID sujet) {
        return jwt().jwt(builder -> builder.subject(sujet.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminTenantJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN_TENANT"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor superAdminJwtAvecSubNonUuid() {
        return jwt().jwt(builder -> builder.subject("service-account-facturation"))
                .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
    }

    private SubscriptionDto.View vueAttendue() {
        return new SubscriptionDto.View(
                ABONNEMENT, CLIENT, "controlplan", BillingTier.STANDARD, BillingPeriod.MONTHLY,
                9900, "EUR", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 15),
                null, null, Instant.parse("2026-09-15T10:00:00Z"), ACTOR);
    }

    @Nested
    @DisplayName("POST — souscrire")
    class Subscribe {

        @Test
        void unAdminDeTenantNeSouscritPasPourSonPropreCompte() throws Exception {
            // Souscrire engage une facturation : c'est un acte d'EDITEUR. Un
            // administrateur de tenant qui pourrait souscrire pourrait aussi
            // resilier la veille de l'echeance.
            mockMvc.perform(post(BASE)
                            .with(adminTenantJwt()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CORPS_VALIDE))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }

        @Test
        void leSuperAdminSouscritPourLeClientDuChemin_avecLActeurDuJeton() throws Exception {
            when(service.subscribe(eq(CLIENT), any(), eq(ACTOR))).thenReturn(vueAttendue());

            mockMvc.perform(post(BASE)
                            .with(superAdminJwtDe(ACTOR)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CORPS_VALIDE))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.tenantId").value(CLIENT.toString()))
                    .andExpect(jsonPath("$.moduleCode").value("controlplan"))
                    .andExpect(jsonPath("$.createdBy").value(ACTOR.toString()));

            verify(service).subscribe(eq(CLIENT), any(), eq(ACTOR));
        }

        @Test
        void unTenantIdGlisseDansLeCorpsNeChangeRien() throws Exception {
            // §18.2 regle 2 : le client vient du CHEMIN. La commande n'a pas de
            // champ tenantId, Jackson ignore donc celui du corps — l'usurpation
            // est impossible par construction, pas filtree par une validation
            // qu'on pourrait oublier ailleurs.
            when(service.subscribe(eq(CLIENT), any(), eq(ACTOR))).thenReturn(vueAttendue());

            mockMvc.perform(post(BASE)
                            .with(superAdminJwtDe(ACTOR)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CORPS_AVEC_TENANT_FORGE))
                    .andExpect(status().isCreated());

            verify(service).subscribe(eq(CLIENT), any(), eq(ACTOR));
        }

        @Test
        void unModuleAbsentEstRefuseAvantDAtteindreLeService() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(superAdminJwtDe(ACTOR)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CORPS_SANS_MODULE))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }

        @Test
        void unSubNonUuidEstRefuseProprement_pasUn500() throws Exception {
            mockMvc.perform(post(BASE)
                            .with(superAdminJwtAvecSubNonUuid()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CORPS_VALIDE))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(service);
        }

        @Test
        void unModuleDejaSouscritRepond409_pas500() throws Exception {
            // La requete est bien formee et le serveur va bien : c'est l'etat qui
            // s'y oppose. Sans handler dedie, ce refus tombait dans le catch-all
            // et ressemblait a une panne.
            when(service.subscribe(eq(CLIENT), any(), eq(ACTOR)))
                    .thenThrow(new IllegalStateException("Module deja souscrit par ce client : controlplan"));

            mockMvc.perform(post(BASE)
                            .with(superAdminJwtDe(ACTOR)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CORPS_VALIDE))
                    .andExpect(status().isConflict());
        }

        @Test
        void unMoteurQuiRefuseRepond502_pas500() throws Exception {
            // La panne n'est pas ici : un 500 enverrait chercher le defaut dans le
            // mauvais service.
            when(service.subscribe(eq(CLIENT), any(), eq(ACTOR)))
                    .thenThrow(new ModuleActivationFailedException("moteur injoignable"));

            mockMvc.perform(post(BASE)
                            .with(superAdminJwtDe(ACTOR)).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(CORPS_VALIDE))
                    .andExpect(status().isBadGateway());
        }
    }

    @Nested
    @DisplayName("DELETE — résilier")
    class Cancel {

        @Test
        void unAdminDeTenantNeResiliePas() throws Exception {
            mockMvc.perform(delete(BASE + "/" + ABONNEMENT).with(adminTenantJwt()).with(csrf()))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }

        @Test
        void leSuperAdminResilie_etRecoitLaDateDEffet() throws Exception {
            // 200 avec le corps plutot que 204 : la date de resiliation decide de
            // la derniere periode facturee. Un 204 obligerait a relire pour
            // l'apprendre.
            SubscriptionDto.View resilie = new SubscriptionDto.View(
                    ABONNEMENT, CLIENT, "controlplan", BillingTier.STANDARD, BillingPeriod.MONTHLY,
                    9900, "EUR", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 15),
                    Instant.parse("2026-09-20T08:00:00Z"), ACTOR,
                    Instant.parse("2026-09-15T10:00:00Z"), ACTOR);
            when(service.cancel(CLIENT, ABONNEMENT, ACTOR)).thenReturn(resilie);

            mockMvc.perform(delete(BASE + "/" + ABONNEMENT)
                            .with(superAdminJwtDe(ACTOR)).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cancelledAt").value("2026-09-20T08:00:00Z"))
                    .andExpect(jsonPath("$.cancelledBy").value(ACTOR.toString()));

            verify(service).cancel(CLIENT, ABONNEMENT, ACTOR);
        }

        @Test
        void unAbonnementInconnuRepond404() throws Exception {
            when(service.cancel(CLIENT, ABONNEMENT, ACTOR))
                    .thenThrow(new SubscriptionNotFoundException(ABONNEMENT));

            mockMvc.perform(delete(BASE + "/" + ABONNEMENT)
                            .with(superAdminJwtDe(ACTOR)).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET — lister")
    class ListSubscriptions {

        @Test
        void unAdminDeTenantNeListePasLesAbonnements() throws Exception {
            mockMvc.perform(get(BASE).with(adminTenantJwt()))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }

        @Test
        void leSuperAdminListeLesAbonnementsVivantsDuClient() throws Exception {
            when(service.activeFor(CLIENT)).thenReturn(List.of(vueAttendue()));

            mockMvc.perform(get(BASE).with(superAdminJwtDe(ACTOR)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].moduleCode").value("controlplan"))
                    .andExpect(jsonPath("$[0].amountCents").value(9900));
        }

        @Test
        void unClientSansAbonnementRenvoieUneListeVide_pasUneErreur() throws Exception {
            when(service.activeFor(CLIENT)).thenReturn(List.of());

            mockMvc.perform(get(BASE).with(superAdminJwtDe(ACTOR)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$").isEmpty());
        }
    }
}
