package com.openlab.qualitos.core.billing.invoice;

import com.openlab.qualitos.core.billing.BillingPeriod;
import com.openlab.qualitos.core.billing.BillingProfileDto;
import com.openlab.qualitos.core.billing.BillingProfileService;
import com.openlab.qualitos.core.billing.BillingTier;
import com.openlab.qualitos.core.billing.SubscriptionDto;
import com.openlab.qualitos.core.billing.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InvoiceService")
class InvoiceServiceTest {

    @Mock InvoiceRepository repo;
    @Mock SubscriptionService subscriptions;
    @Mock BillingProfileService profiles;
    @Mock InvoiceRenderPort renderer;
    @Mock InvoiceMailPort mailer;

    InvoiceService service;

    static final UUID CLIENT = UUID.randomUUID();
    static final UUID ACTEUR = UUID.randomUUID();
    static final UUID FACTURE_ID = UUID.randomUUID();
    static final YearMonth SEPTEMBRE = YearMonth.of(2026, 9);
    static final Instant NOW = Instant.parse("2026-10-01T06:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new InvoiceService(repo, subscriptions, profiles, renderer, mailer, CLOCK);
        when(repo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(repo.findByTenantAndPeriod(any(), anyYear(), anyMonth())).thenReturn(Optional.empty());
    }

    private static int anyYear() {
        return org.mockito.ArgumentMatchers.anyInt();
    }

    private static int anyMonth() {
        return org.mockito.ArgumentMatchers.anyInt();
    }

    @Nested
    @DisplayName("émission")
    class Issue {

        @Test
        void unClientExempteNeRecoitPasDeFacture() {
            // Le compte de demonstration. Emettre une facture a zero euro lui
            // ferait croire a un contrat.
            when(profiles.isExempt(CLIENT)).thenReturn(true);

            assertThat(service.issueFor(CLIENT, SEPTEMBRE, ACTEUR)).isEmpty();

            verify(repo, never()).save(any());
            verifyNoInteractions(subscriptions);
        }

        @Test
        void uneFactureReprendLePrixFigeDeLAbonnement() {
            // Le client a signe a 99 EUR ; le tarif est passe a 129. La facture
            // doit dire 99, sinon on facture unilateralement une hausse. Ce
            // service ne connait meme pas ModulePriceService : ne pas avoir la
            // dependance est plus sur que se rappeler de ne pas s'en servir.
            when(subscriptions.activeFor(CLIENT)).thenReturn(List.of(mensuel(9900)));

            InvoiceDto.View facture = service.issueFor(CLIENT, SEPTEMBRE, ACTEUR).orElseThrow();

            assertThat(facture.lines()).hasSize(1);
            assertThat(facture.lines().get(0).lineTotalCents()).isEqualTo(9900);
            assertThat(facture.totalCents()).isEqualTo(9900);
            assertThat(facture.currency()).isEqualTo("EUR");
        }

        @Test
        void laFacturePorteSonEmetteurSaPeriodeEtSonNumero() {
            when(subscriptions.activeFor(CLIENT)).thenReturn(List.of(mensuel(9900)));
            when(repo.findLastNumberOfFiscalYear(2026)).thenReturn(Optional.of("FA-2026-0041"));

            InvoiceDto.View facture = service.issueFor(CLIENT, SEPTEMBRE, ACTEUR).orElseThrow();

            assertThat(facture.number()).isEqualTo("FA-2026-0042");
            assertThat(facture.fiscalYear()).isEqualTo(2026);
            assertThat(facture.period()).isEqualTo(SEPTEMBRE);
            assertThat(facture.issuedBy()).isEqualTo(ACTEUR);
            assertThat(facture.issuedAt()).isEqualTo(NOW);
            assertThat(facture.sentAt()).isNull();
        }

        @Test
        void laPremiereFactureDUnExerciceOuvreLaSequence() {
            when(subscriptions.activeFor(CLIENT)).thenReturn(List.of(mensuel(9900)));
            when(repo.findLastNumberOfFiscalYear(2026)).thenReturn(Optional.empty());

            assertThat(service.issueFor(CLIENT, SEPTEMBRE, ACTEUR).orElseThrow().number())
                    .isEqualTo("FA-2026-0001");
        }

        @Test
        void plusieursAbonnementsFontPlusieursLignesEtUnTotalAdditionne() {
            when(subscriptions.activeFor(CLIENT))
                    .thenReturn(List.of(mensuel(9900), mensuel(1500)));

            InvoiceDto.View facture = service.issueFor(CLIENT, SEPTEMBRE, ACTEUR).orElseThrow();

            assertThat(facture.lines()).hasSize(2);
            assertThat(facture.lines()).extracting(InvoiceDto.LineView::lineNo)
                    .containsExactly(1, 2);
            assertThat(facture.totalCents()).isEqualTo(11400);
        }

        @Test
        void deuxDevisesDifferentesFontEchouerLEmission() {
            // Additionner des euros a des dollars donne un nombre qui ne veut
            // rien dire. Money le refuse ; on veut que ce refus remonte plutot
            // qu'un total silencieusement faux.
            when(subscriptions.activeFor(CLIENT))
                    .thenReturn(List.of(mensuel(9900), abonnement(1500, BillingPeriod.MONTHLY,
                            LocalDate.of(2026, 3, 15), "USD")));

            assertThatThrownBy(() -> service.issueFor(CLIENT, SEPTEMBRE, ACTEUR))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("devises differentes");
        }

        @Test
        void unAbonnementAnnuelNEstPasFactureChaqueMois() {
            // Sinon on facture douze fois un contrat annuel — la faute la plus
            // couteuse que ce service puisse commettre.
            when(subscriptions.activeFor(CLIENT)).thenReturn(List.of(
                    abonnement(99000, BillingPeriod.ANNUAL, LocalDate.of(2026, 3, 15), "EUR")));

            assertThat(service.issueFor(CLIENT, SEPTEMBRE, ACTEUR)).isEmpty();
            verify(repo, never()).save(any());
        }

        @Test
        void unAbonnementAnnuelEstFactureLeMoisAnniversaire() {
            when(subscriptions.activeFor(CLIENT)).thenReturn(List.of(
                    abonnement(99000, BillingPeriod.ANNUAL, LocalDate.of(2026, 3, 15), "EUR")));

            InvoiceDto.View facture =
                    service.issueFor(CLIENT, YearMonth.of(2027, 3), ACTEUR).orElseThrow();

            assertThat(facture.totalCents()).isEqualTo(99000);
        }

        @Test
        void unAbonnementNEstPasFacturePourUnePeriodeAnterieureASonDebut() {
            // Reemettre les factures d'un exercice ancien ne doit pas facturer
            // un contrat signe depuis.
            when(subscriptions.activeFor(CLIENT)).thenReturn(List.of(
                    abonnement(9900, BillingPeriod.MONTHLY, LocalDate.of(2026, 3, 15), "EUR")));

            assertThat(service.issueFor(CLIENT, YearMonth.of(2026, 1), ACTEUR)).isEmpty();
        }

        @Test
        void unClientSansAbonnementDuNeRecoitPasDeFactureVide() {
            // Une facture vide n'a rien a dire, et son numero consommerait un
            // rang de la sequence pour rien — donc un trou.
            when(subscriptions.activeFor(CLIENT)).thenReturn(List.of());

            assertThat(service.issueFor(CLIENT, SEPTEMBRE, ACTEUR)).isEmpty();
            verify(repo, never()).save(any());
        }

        @Test
        void deuxEmissionsPourLaMemePeriodeNeFontPasDeuxFactures() {
            // L'idempotence : relancer le traitement mensuel, apres une panne ou
            // par prudence, ne doit pas doubler la facturation.
            when(repo.findByTenantAndPeriod(CLIENT, 2026, 9))
                    .thenReturn(Optional.of(factureExistante()));

            InvoiceDto.View facture = service.issueFor(CLIENT, SEPTEMBRE, ACTEUR).orElseThrow();

            assertThat(facture.number()).isEqualTo("FA-2026-0007");
            verify(repo, never()).save(any());
        }

        @Test
        void uneFactureDejaEmiseNeDisparaitPasSiLeClientDevientExempte() {
            // L'exemption est lue APRES la recherche de l'existante : exempter
            // un client ne fait pas disparaitre ce qu'il a deja recu.
            when(repo.findByTenantAndPeriod(CLIENT, 2026, 9))
                    .thenReturn(Optional.of(factureExistante()));
            when(profiles.isExempt(CLIENT)).thenReturn(true);

            assertThat(service.issueFor(CLIENT, SEPTEMBRE, ACTEUR)).isPresent();
        }
    }

    @Nested
    @DisplayName("rendu et envoi")
    class Send {

        @Test
        void laFactureEstEnvoyeeAuDestinataireDeFacturation() {
            // Et non a l'administrateur du tenant : la comptabilite n'est pas
            // l'informatique, et une facture qui arrive chez l'admin systeme
            // attend souvent qu'on la relance pour etre payee.
            Invoice facture = factureExistante();
            when(repo.findById(FACTURE_ID)).thenReturn(Optional.of(facture));
            when(profiles.find(CLIENT)).thenReturn(Optional.of(profil("compta@acme.example")));
            when(renderer.render(any(), any())).thenReturn("PDF".getBytes());

            service.send(FACTURE_ID, ACTEUR);

            verify(mailer).send(eq("compta@acme.example"), any(), any(), any());
        }

        @Test
        void lEnvoiEstHorodateEtNommeSonDestinataire() {
            Invoice facture = factureExistante();
            when(repo.findById(FACTURE_ID)).thenReturn(Optional.of(facture));
            when(profiles.find(CLIENT)).thenReturn(Optional.of(profil("compta@acme.example")));
            when(renderer.render(any(), any())).thenReturn("PDF".getBytes());

            InvoiceDto.View vue = service.send(FACTURE_ID, ACTEUR);

            assertThat(vue.sentAt()).isEqualTo(NOW);
            assertThat(vue.sentTo()).isEqualTo("compta@acme.example");
        }

        @Test
        void lePdfPartEnPieceJointe() {
            Invoice facture = factureExistante();
            when(repo.findById(FACTURE_ID)).thenReturn(Optional.of(facture));
            when(profiles.find(CLIENT)).thenReturn(Optional.of(profil("compta@acme.example")));
            when(renderer.render(any(), any())).thenReturn(new byte[] {1, 2, 3});

            service.send(FACTURE_ID, ACTEUR);

            ArgumentCaptor<byte[]> piece = ArgumentCaptor.forClass(byte[].class);
            verify(mailer).send(any(), any(), any(), piece.capture());
            assertThat(piece.getValue()).containsExactly(1, 2, 3);
        }

        @Test
        void leCorpsPorteLeMontantEnClairEtLaRaisonSociale() {
            // 9900 centimes s'ecrit 99,00 EUR. Un corps qui dirait « 9900 EUR »
            // reclamerait cent fois trop, et aucune assertion sur des entiers
            // ne le verrait.
            Invoice facture = factureExistante();
            when(repo.findById(FACTURE_ID)).thenReturn(Optional.of(facture));
            when(profiles.find(CLIENT)).thenReturn(Optional.of(profil("compta@acme.example")));
            when(renderer.render(any(), any())).thenReturn("PDF".getBytes());

            service.send(FACTURE_ID, ACTEUR);

            ArgumentCaptor<String> corps = ArgumentCaptor.forClass(String.class);
            verify(mailer).send(any(), any(), corps.capture(), any());
            assertThat(corps.getValue()).contains("99,00 EUR").contains("ACME Industries SAS");
        }

        @Test
        void uneFactureDejaEnvoyeeNeSeRenvoiePasSeule() {
            // Deux exemplaires de la meme facture, c'est un litige : le client
            // ne sait pas s'il doit payer une fois ou deux.
            Invoice envoyee = factureExistante();
            envoyee.markSent("compta@acme.example", Instant.parse("2026-10-01T07:00:00Z"));
            when(repo.findById(FACTURE_ID)).thenReturn(Optional.of(envoyee));

            assertThatThrownBy(() -> service.send(FACTURE_ID, ACTEUR))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("deja envoyee");

            verifyNoInteractions(mailer);
            // Le refus tombe avant le rendu : inutile de fabriquer un PDF qui
            // ne partira pas.
            verifyNoInteractions(renderer);
        }

        @Test
        void siLEnvoiEchoueLaFactureResteNonEnvoyee() {
            // Sans quoi une panne SMTP passagere marquerait la facture partie,
            // et le client attendrait une piece que personne ne lui renverrait.
            Invoice facture = factureExistante();
            when(repo.findById(FACTURE_ID)).thenReturn(Optional.of(facture));
            when(profiles.find(CLIENT)).thenReturn(Optional.of(profil("compta@acme.example")));
            when(renderer.render(any(), any())).thenReturn("PDF".getBytes());
            doThrow(new IllegalStateException("relais SMTP injoignable"))
                    .when(mailer).send(any(), any(), any(), any());

            assertThatThrownBy(() -> service.send(FACTURE_ID, ACTEUR))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(facture.isSent()).isFalse();
            verify(repo, never()).save(any());
        }

        @Test
        void sansProfilDeFacturationOnRefuseAuLieuDEditerUnUuid() {
            // « On ne facture pas un UUID » : sans profil, la piece n'aurait ni
            // raison sociale, ni adresse, ni destinataire.
            when(repo.findById(FACTURE_ID)).thenReturn(Optional.of(factureExistante()));
            when(profiles.find(CLIENT)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.send(FACTURE_ID, ACTEUR))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("profil de facturation");
        }

        @Test
        void leRenduPdfPasseParLePortAvecLeProfilDuClient() {
            when(repo.findById(FACTURE_ID)).thenReturn(Optional.of(factureExistante()));
            BillingProfileDto.View profil = profil("compta@acme.example");
            when(profiles.find(CLIENT)).thenReturn(Optional.of(profil));
            when(renderer.render(any(), eq(profil))).thenReturn(new byte[] {9});

            assertThat(service.renderPdf(FACTURE_ID)).containsExactly(9);
        }

        @Test
        void uneFactureInconnueEstUn404() {
            when(repo.findById(FACTURE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.send(FACTURE_ID, ACTEUR))
                    .isInstanceOf(InvoiceNotFoundException.class);
            assertThatThrownBy(() -> service.renderPdf(FACTURE_ID))
                    .isInstanceOf(InvoiceNotFoundException.class);
            assertThatThrownBy(() -> service.get(FACTURE_ID))
                    .isInstanceOf(InvoiceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("lecture")
    class Read {

        @Test
        void lesFacturesDUnClientSeLisentLaPlusRecenteEnTete() {
            when(repo.findByTenantOrderByNumberDesc(CLIENT))
                    .thenReturn(List.of(factureExistante()));

            List<InvoiceDto.View> vues = service.findByTenant(CLIENT);

            assertThat(vues).hasSize(1);
            assertThat(vues.get(0).number()).isEqualTo("FA-2026-0007");
        }

        @Test
        void uneFacturePreciseSeLitParSonIdentifiant() {
            when(repo.findById(FACTURE_ID)).thenReturn(Optional.of(factureExistante()));

            assertThat(service.get(FACTURE_ID).number()).isEqualTo("FA-2026-0007");
        }
    }

    // ---------- fixtures ----------

    private static SubscriptionDto.View mensuel(long cents) {
        return abonnement(cents, BillingPeriod.MONTHLY, LocalDate.of(2026, 3, 15), "EUR");
    }

    private static SubscriptionDto.View abonnement(long cents, BillingPeriod period,
                                                    LocalDate startedOn, String currency) {
        return new SubscriptionDto.View(
                UUID.randomUUID(), CLIENT, "controlplan", BillingTier.STANDARD, period,
                cents, currency, startedOn, period.nextRenewal(startedOn),
                null, null, Instant.parse("2026-03-15T10:00:00Z"), ACTEUR);
    }

    private static Invoice factureExistante() {
        return Invoice.builder()
                .id(FACTURE_ID)
                .tenantId(CLIENT)
                .number("FA-2026-0007")
                .fiscalYear(2026)
                .periodYear(2026)
                .periodMonth(9)
                .currency("EUR")
                .totalCents(9900)
                .issuedAt(Instant.parse("2026-10-01T06:00:00Z"))
                .issuedBy(ACTEUR)
                .lines(new java.util.ArrayList<>(List.of(InvoiceLine.builder()
                        .id(UUID.randomUUID())
                        .subscriptionId(UUID.randomUUID())
                        .lineNo(1)
                        .moduleCode("controlplan")
                        .billingTier(BillingTier.STANDARD)
                        .period(BillingPeriod.MONTHLY)
                        .quantity(1)
                        .unitAmountCents(9900)
                        .lineTotalCents(9900)
                        .build())))
                .build();
    }

    private static BillingProfileDto.View profil(String billingEmail) {
        return new BillingProfileDto.View(
                UUID.randomUUID(), CLIENT, "ACME Industries SAS", "FR12345678901",
                "1 rue de la Facture", null, "75000", "Paris", "FR",
                billingEmail, "EUR", false, null,
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"));
    }
}
