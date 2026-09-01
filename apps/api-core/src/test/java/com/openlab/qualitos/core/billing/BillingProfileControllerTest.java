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
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Le brief cite {@code @Import(MethodSecurityTestConfig.class)} : cette classe
 * vit dans le moteur de qualité (api-quality-engine), pas dans api-core, et un
 * module Maven frère ne peut pas l'importer. On reprend donc la convention
 * déjà en place dans {@code TenantControllerTest} : monter la VRAIE
 * {@link SecurityConfig} de ce module (pour exercer la même chaîne de filtres
 * qu'en production, {@code @PreAuthorize} compris) et le
 * {@link GlobalExceptionHandler} pour la traduction des exceptions en
 * réponses HTTP.
 */
@WebMvcTest(BillingProfileController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("BillingProfileController")
class BillingProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BillingProfileService service;

    // SecurityConfig déclare un bean TenantJwtFilter(JwtDecoder) : sans ce
    // mock, le contexte @WebMvcTest ne démarre pas — même motif que dans
    // TenantControllerTest. Le filtre lui-même est un no-op ici : les tests
    // n'envoient aucun en-tête Authorization, @WithMockUser pose directement
    // l'Authentication dans le contexte de sécurité.
    @MockBean
    private JwtDecoder jwtDecoder;

    private static final UUID CLIENT = UUID.randomUUID();

    private static final String CORPS_VALIDE = """
            {
              "legalName": "ACME Industries SAS",
              "vatNumber": "FR12345678901",
              "addressLine1": "12 rue de la Paix",
              "addressLine2": null,
              "postalCode": "75002",
              "city": "Paris",
              "countryCode": "FR",
              "billingEmail": "billing@acme-industries.example",
              "currency": "EUR",
              "billingExempt": false,
              "exemptionReason": null
            }
            """;

    // Corps portant un AUTRE identifiant de client que celui du chemin. Le
    // champ "tenantId" n'existe pas dans SaveCommand : Jackson (config par
    // défaut de Spring Boot) l'ignore silencieusement au lieu de rejeter la
    // requête — c'est ce qui rend l'usurpation impossible plutôt que filtrée.
    private static String corpsAvecTenantId(UUID autre) {
        return """
                {
                  "tenantId": "%s",
                  "legalName": "ACME Industries SAS",
                  "vatNumber": "FR12345678901",
                  "addressLine1": "12 rue de la Paix",
                  "addressLine2": null,
                  "postalCode": "75002",
                  "city": "Paris",
                  "countryCode": "FR",
                  "billingEmail": "billing@acme-industries.example",
                  "currency": "EUR",
                  "billingExempt": false,
                  "exemptionReason": null
                }
                """.formatted(autre);
    }

    private BillingProfileDto.View vueAttendue() {
        Instant now = Instant.now();
        return new BillingProfileDto.View(
                UUID.randomUUID(),
                CLIENT,
                "ACME Industries SAS",
                "FR12345678901",
                "12 rue de la Paix",
                null,
                "75002",
                "Paris",
                "FR",
                "billing@acme-industries.example",
                "EUR",
                false,
                null,
                now,
                now
        );
    }

    @Nested
    @DisplayName("PUT .../billing-profile")
    class SaveBillingProfile {

        @Test
        @WithMockUser(roles = "ADMIN_TENANT")
        void unAdminDeTenantNeGerePasLaFacturationDUnClient() throws Exception {
            // La facturation est une affaire d'EDITEUR. Un administrateur de tenant
            // qui pourrait editer son propre profil pourrait s'exempter lui-meme.
            mockMvc.perform(put("/api/v1/admin/clients/" + CLIENT + "/billing-profile")
                            .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(CORPS_VALIDE))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        void leSuperAdminEnregistreLeProfilDUnClientDesigne() throws Exception {
            when(service.upsert(eq(CLIENT), any())).thenReturn(vueAttendue());

            mockMvc.perform(put("/api/v1/admin/clients/" + CLIENT + "/billing-profile")
                            .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(CORPS_VALIDE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.legalName").value("ACME Industries SAS"));
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        void leClientEstDesigneParLeCHEMIN_jamaisParLeCorps() throws Exception {
            // §18.2 regle 2. Le corps porte un autre identifiant : il doit etre
            // IGNORE, sans quoi on pourrait facturer un client en se faisant passer
            // pour un autre.
            UUID autre = UUID.randomUUID();
            when(service.upsert(eq(CLIENT), any())).thenReturn(vueAttendue());

            mockMvc.perform(put("/api/v1/admin/clients/" + CLIENT + "/billing-profile")
                            .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(corpsAvecTenantId(autre)))
                    .andExpect(status().isOk());

            verify(service).upsert(eq(CLIENT), any());
            verify(service, never()).upsert(eq(autre), any());
        }
    }

    // Le brief ne donne que les 3 tests PUT ci-dessus, mais "Interfaces
    // produites" liste aussi le GET — c'est la meme surface d'administration,
    // reservee au meme role. Ces tests couvrent les deux branches (trouve /
    // absent) du controleur, exigees par le seuil de couverture du depot.
    @Nested
    @DisplayName("GET .../billing-profile")
    class GetBillingProfile {

        @Test
        @WithMockUser(roles = "ADMIN_TENANT")
        void unAdminDeTenantNeConsultePasLaFacturationDUnClient() throws Exception {
            mockMvc.perform(get("/api/v1/admin/clients/" + CLIENT + "/billing-profile"))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        void leSuperAdminConsulteLeProfilDUnClientDesigne() throws Exception {
            when(service.find(CLIENT)).thenReturn(Optional.of(vueAttendue()));

            mockMvc.perform(get("/api/v1/admin/clients/" + CLIENT + "/billing-profile"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.legalName").value("ACME Industries SAS"));
        }

        @Test
        @WithMockUser(roles = "SUPER_ADMIN")
        void retourne404QuandAucunProfilNExistePourCeClient() throws Exception {
            // L'absence de profil n'est pas une erreur serveur : c'est un
            // client encore a renseigner (regle 4 du service).
            when(service.find(CLIENT)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v1/admin/clients/" + CLIENT + "/billing-profile"))
                    .andExpect(status().isNotFound());
        }
    }
}
