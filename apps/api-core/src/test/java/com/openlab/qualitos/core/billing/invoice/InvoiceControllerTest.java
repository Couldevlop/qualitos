package com.openlab.qualitos.core.billing.invoice;

import com.openlab.qualitos.core.billing.BillingPeriod;
import com.openlab.qualitos.core.billing.BillingTier;
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
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Même convention que {@code SubscriptionControllerTest} : la vraie
 * {@link SecurityConfig} est montée, et l'authentification passe par
 * {@code jwt()} — le contrôleur résout l'émetteur avec
 * {@code CurrentUser.requireUserId()}.
 */
@WebMvcTest(InvoiceController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("InvoiceController")
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InvoiceService service;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID CLIENT = UUID.randomUUID();
    private static final UUID FACTURE = UUID.randomUUID();
    private static final YearMonth SEPTEMBRE = YearMonth.of(2026, 9);

    private static final String CLIENT_INVOICES = "/api/v1/admin/clients/" + CLIENT + "/invoices";
    private static final String INVOICE = "/api/v1/admin/invoices/" + FACTURE;

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor superAdminJwtDe(UUID sujet) {
        return jwt().jwt(builder -> builder.subject(sujet.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
    }

    private SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor adminTenantJwt() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN_TENANT"));
    }

    private static InvoiceDto.View vue() {
        return new InvoiceDto.View(
                FACTURE, CLIENT, "FA-2026-0007", 2026, SEPTEMBRE, "EUR", 9900,
                Instant.parse("2026-10-01T06:00:00Z"), ACTOR, null, null,
                List.of(new InvoiceDto.LineView(1, UUID.randomUUID(), "controlplan",
                        BillingTier.STANDARD, BillingPeriod.MONTHLY, 1, 9900, 9900)));
    }

    @Nested
    @DisplayName("POST — émettre")
    class Issue {

        @Test
        void unAdminDeTenantNEmetPasSesPropresFactures() throws Exception {
            // Un client qui pourrait emettre ses factures pourrait s'en
            // dispenser.
            mockMvc.perform(post(CLIENT_INVOICES).param("period", "2026-09")
                            .with(adminTenantJwt()).with(csrf()))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }

        @Test
        void leSuperAdminEmetPourLeClientDuChemin_avecLActeurDuJeton() throws Exception {
            when(service.issueFor(eq(CLIENT), eq(SEPTEMBRE), eq(ACTOR)))
                    .thenReturn(Optional.of(vue()));

            mockMvc.perform(post(CLIENT_INVOICES).param("period", "2026-09")
                            .with(superAdminJwtDe(ACTOR)).with(csrf()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.number").value("FA-2026-0007"))
                    .andExpect(jsonPath("$.totalCents").value(9900))
                    .andExpect(jsonPath("$.issuedBy").value(ACTOR.toString()));

            verify(service).issueFor(CLIENT, SEPTEMBRE, ACTOR);
        }

        @Test
        void rienAFacturerRepond204_pas404NiUnCorpsVide() throws Exception {
            // Client exempte, ou aucun abonnement du ce mois-la : ce n'est pas
            // une erreur, c'est le cas ordinaire du compte de demonstration. Un
            // 404 laisserait croire a une adresse fausse, un 200 vide
            // obligerait l'appelant a deviner.
            when(service.issueFor(eq(CLIENT), eq(SEPTEMBRE), eq(ACTOR)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post(CLIENT_INVOICES).param("period", "2026-09")
                            .with(superAdminJwtDe(ACTOR)).with(csrf()))
                    .andExpect(status().isNoContent());
        }

        @Test
        void unePeriodeMalFormeeEstRefuseeAvantDAtteindreLeService() throws Exception {
            mockMvc.perform(post(CLIENT_INVOICES).param("period", "septembre")
                            .with(superAdminJwtDe(ACTOR)).with(csrf()))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(service);
        }

        @Test
        void unSubNonUuidEstRefuseProprement_pasUn500() throws Exception {
            mockMvc.perform(post(CLIENT_INVOICES).param("period", "2026-09")
                            .with(jwt().jwt(b -> b.subject("service-account-facturation"))
                                    .authorities(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")))
                            .with(csrf()))
                    .andExpect(status().isUnauthorized());
            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("lecture et PDF")
    class Read {

        @Test
        void unAdminDeTenantNeLitPasLesFacturesParLaSurfaceEditeur() throws Exception {
            mockMvc.perform(get(CLIENT_INVOICES).with(adminTenantJwt()))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }

        @Test
        void leSuperAdminListeLesFacturesDUnClient() throws Exception {
            when(service.findByTenant(CLIENT)).thenReturn(List.of(vue()));

            mockMvc.perform(get(CLIENT_INVOICES).with(superAdminJwtDe(ACTOR)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].number").value("FA-2026-0007"));
        }

        @Test
        void unClientSansFactureRenvoieUneListeVide_pasUneErreur() throws Exception {
            when(service.findByTenant(CLIENT)).thenReturn(List.of());

            mockMvc.perform(get(CLIENT_INVOICES).with(superAdminJwtDe(ACTOR)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        void uneFactureSeLitAvecSesLignes() throws Exception {
            when(service.get(FACTURE)).thenReturn(vue());

            mockMvc.perform(get(INVOICE).with(superAdminJwtDe(ACTOR)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lines[0].moduleCode").value("controlplan"));
        }

        @Test
        void unePieceInconnueRepond404() throws Exception {
            when(service.get(FACTURE)).thenThrow(new InvoiceNotFoundException(FACTURE));

            mockMvc.perform(get(INVOICE).with(superAdminJwtDe(ACTOR)))
                    .andExpect(status().isNotFound());
        }

        @Test
        void lePdfSortEnLigne_nommeParLeNumeroDeFacture() throws Exception {
            // « inline » : l'editeur veut RELIRE la piece avant de l'envoyer,
            // et une piece jointe forcerait un aller-retour par le disque a
            // chaque verification.
            when(service.get(FACTURE)).thenReturn(vue());
            when(service.renderPdf(FACTURE)).thenReturn("%PDF-1.4".getBytes());

            mockMvc.perform(get(INVOICE + "/pdf").with(superAdminJwtDe(ACTOR)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                    .andExpect(header().string("Content-Disposition",
                            org.hamcrest.Matchers.containsString("FA-2026-0007.pdf")))
                    .andExpect(header().string("Content-Disposition",
                            org.hamcrest.Matchers.startsWith("inline")));
        }

        @Test
        void unAdminDeTenantNeTelechargePasLePdf() throws Exception {
            mockMvc.perform(get(INVOICE + "/pdf").with(adminTenantJwt()))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }
    }

    @Nested
    @DisplayName("POST — envoyer")
    class Send {

        @Test
        void unAdminDeTenantNEnvoiePasLesFactures() throws Exception {
            mockMvc.perform(post(INVOICE + "/send").with(adminTenantJwt()).with(csrf()))
                    .andExpect(status().isForbidden());
            verifyNoInteractions(service);
        }

        @Test
        void leSuperAdminEnvoie_avecLActeurDuJeton() throws Exception {
            when(service.send(FACTURE, ACTOR)).thenReturn(vue());

            mockMvc.perform(post(INVOICE + "/send").with(superAdminJwtDe(ACTOR)).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.number").value("FA-2026-0007"));

            verify(service).send(FACTURE, ACTOR);
        }

        @Test
        void uneFactureDejaEnvoyeeRepond409_pas500() throws Exception {
            when(service.send(FACTURE, ACTOR))
                    .thenThrow(new IllegalStateException("Facture FA-2026-0007 deja envoyee"));

            mockMvc.perform(post(INVOICE + "/send").with(superAdminJwtDe(ACTOR)).with(csrf()))
                    .andExpect(status().isConflict());
        }
    }
}
