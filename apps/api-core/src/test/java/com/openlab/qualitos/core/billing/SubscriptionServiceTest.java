package com.openlab.qualitos.core.billing;

import com.openlab.qualitos.core.tenant.TenantNotFoundException;
import com.openlab.qualitos.core.tenant.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.openlab.qualitos.core.billing.BillingPeriod.ANNUAL;
import static com.openlab.qualitos.core.billing.BillingPeriod.MONTHLY;
import static com.openlab.qualitos.core.billing.BillingTier.STANDARD;
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
class SubscriptionServiceTest {

    @Mock SubscriptionRepository repo;
    @Mock ModulePriceService prices;
    @Mock ModuleActivationPort activation;
    @Mock TenantRepository tenants;

    SubscriptionService service;

    static final UUID CLIENT = UUID.randomUUID();
    static final UUID ACTEUR = UUID.randomUUID();
    static final UUID ID = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-09-15T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    static final SubscriptionDto.SubscribeCommand CONTROLPLAN =
            new SubscriptionDto.SubscribeCommand("controlplan", STANDARD, MONTHLY);

    @BeforeEach
    void setup() {
        service = new SubscriptionService(repo, prices, activation, tenants, CLOCK);
    }

    // ---------- souscription ----------

    @Test
    void lePrixEstFigeALaSouscription() {
        // Une hausse de tarif ne doit pas reecrire le montant d'un contrat signe :
        // le montant est RECOPIE dans la ligne, pas relu du catalogue a chaque
        // lecture.
        clientConnu();
        when(prices.priceOf("controlplan", STANDARD, MONTHLY))
                .thenReturn(Optional.of(Money.of(9900, "EUR")));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.subscribe(CLIENT, CONTROLPLAN, ACTEUR);

        ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getAmountCents()).isEqualTo(9900);
        assertThat(saved.getValue().getCurrency()).isEqualTo("EUR");
    }

    @Test
    void laSouscriptionPorteSonActeurEtSonEcheance() {
        clientConnu();
        when(prices.priceOf("controlplan", STANDARD, MONTHLY))
                .thenReturn(Optional.of(Money.of(9900, "EUR")));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SubscriptionDto.View vue = service.subscribe(CLIENT, CONTROLPLAN, ACTEUR);

        assertThat(vue.tenantId()).isEqualTo(CLIENT);
        assertThat(vue.createdBy()).isEqualTo(ACTEUR);
        assertThat(vue.createdAt()).isEqualTo(NOW);
        assertThat(vue.startedOn()).isEqualTo(LocalDate.of(2026, 9, 15));
        // Mensuel : l'echeance tombe un mois plus tard, pas dans un an.
        assertThat(vue.nextRenewal()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(vue.cancelledAt()).isNull();
        assertThat(vue.amount()).isEqualTo(Money.of(9900, "EUR"));
    }

    @Test
    void unAbonnementAnnuelPorteUneEcheanceAUnAn() {
        clientConnu();
        when(prices.priceOf("controlplan", STANDARD, ANNUAL))
                .thenReturn(Optional.of(Money.of(99000, "EUR")));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SubscriptionDto.View vue = service.subscribe(CLIENT,
                new SubscriptionDto.SubscribeCommand("controlplan", STANDARD, ANNUAL), ACTEUR);

        assertThat(vue.nextRenewal()).isEqualTo(LocalDate.of(2027, 9, 15));
    }

    @Test
    void onNeSouscritPasUnModuleSansTarif() {
        // Optional.empty() du catalogue veut dire « tarif inconnu », pas
        // « gratuit » : en faire un abonnement a zero euro rendrait la perte
        // invisible jusqu'a la lecture de la facture.
        clientConnu();
        when(prices.priceOf("nouveau", STANDARD, MONTHLY)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.subscribe(CLIENT,
                new SubscriptionDto.SubscribeCommand("nouveau", STANDARD, MONTHLY), ACTEUR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("tarif");

        verify(repo, never()).save(any());
        verifyNoInteractions(activation);
    }

    @Test
    void unModuleDejaSouscritNeSeSouscritPasDeuxFois() {
        // Deux abonnements vivants pour le meme module donneraient deux lignes
        // de facture pour une seule prestation.
        clientConnu();
        when(repo.findLiveByTenantAndModule(CLIENT, "controlplan"))
                .thenReturn(Optional.of(abonnementVivant()));

        assertThatThrownBy(() -> service.subscribe(CLIENT, CONTROLPLAN, ACTEUR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deja souscrit");

        verify(repo, never()).save(any());
        // Le refus tombe AVANT l'appel au moteur : inutile d'ouvrir un module
        // deja ouvert, et inutile de deranger le moteur pour l'apprendre.
        verifyNoInteractions(activation);
    }

    @Test
    void onNeSouscritRienPourUnClientInconnu() {
        when(tenants.existsById(CLIENT)).thenReturn(false);

        assertThatThrownBy(() -> service.subscribe(CLIENT, CONTROLPLAN, ACTEUR))
                .isInstanceOf(TenantNotFoundException.class);

        verify(repo, never()).save(any());
        verifyNoInteractions(activation);
    }

    @Test
    void souscrireDeclencheLActivationDansLeMoteur() {
        clientConnu();
        when(prices.priceOf(any(), any(), any())).thenReturn(Optional.of(Money.of(9900, "EUR")));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.subscribe(CLIENT, CONTROLPLAN, ACTEUR);

        verify(activation).activate(CLIENT, "controlplan");
    }

    @Test
    void siLActivationEchoueLAbonnementNEstPasEnregistre() {
        // Sinon on facturerait un module que le client ne peut pas utiliser.
        clientConnu();
        when(prices.priceOf(any(), any(), any())).thenReturn(Optional.of(Money.of(9900, "EUR")));
        doThrow(new ModuleActivationFailedException("dependance manquante"))
                .when(activation).activate(CLIENT, "controlplan");

        assertThatThrownBy(() -> service.subscribe(CLIENT, CONTROLPLAN, ACTEUR))
                .isInstanceOf(ModuleActivationFailedException.class);

        verify(repo, never()).save(any());
    }

    // ---------- resiliation ----------

    @Test
    void unAbonnementResilieResteEnBase() {
        // L'historique justifie les factures deja emises. Le supprimer rendrait
        // inexplicable une ligne de facture passee.
        Subscription vivant = abonnementVivant();
        when(repo.findById(ID)).thenReturn(Optional.of(vivant));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        SubscriptionDto.View vue = service.cancel(CLIENT, ID, ACTEUR);

        verify(repo, never()).delete(any());
        assertThat(vivant.getCancelledAt()).isEqualTo(NOW);
        assertThat(vivant.getCancelledBy()).isEqualTo(ACTEUR);
        assertThat(vue.cancelledAt()).isEqualTo(NOW);
    }

    @Test
    void resilierFermeLeModuleDansLeMoteur() {
        when(repo.findById(ID)).thenReturn(Optional.of(abonnementVivant()));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.cancel(CLIENT, ID, ACTEUR);

        verify(activation).deactivate(CLIENT, "controlplan");
    }

    @Test
    void siLaFermetureEchoueLAbonnementResteVivant() {
        // Le moteur est appele AVANT la mutation, et pas seulement avant le
        // save() : muter l'entite suffirait a la faire ecrire au flush de la
        // transaction, meme sans appel au depot.
        Subscription vivant = abonnementVivant();
        when(repo.findById(ID)).thenReturn(Optional.of(vivant));
        doThrow(new ModuleActivationFailedException("moteur injoignable"))
                .when(activation).deactivate(CLIENT, "controlplan");

        assertThatThrownBy(() -> service.cancel(CLIENT, ID, ACTEUR))
                .isInstanceOf(ModuleActivationFailedException.class);

        assertThat(vivant.isLive()).isTrue();
        assertThat(vivant.getCancelledAt()).isNull();
        verify(repo, never()).save(any());
    }

    @Test
    void unAbonnementDejaResilieNeSeResiliePasDeuxFois() {
        // Reecrire la date effacerait celle qui fait foi devant le client.
        Subscription resilie = abonnementVivant();
        resilie.cancel(ACTEUR, Instant.parse("2026-08-01T00:00:00Z"));
        when(repo.findById(ID)).thenReturn(Optional.of(resilie));

        assertThatThrownBy(() -> service.cancel(CLIENT, ID, ACTEUR))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deja resilie");

        assertThat(resilie.getCancelledAt()).isEqualTo(Instant.parse("2026-08-01T00:00:00Z"));
        // Le refus tombe avant le moteur : fermer un module deja ferme fermerait
        // celui qu'une souscription plus recente aurait rouvert.
        verifyNoInteractions(activation);
    }

    @Test
    void onNeResiliePasLAbonnementDUnAutreClient() {
        // Sur le chemin du client A, l'abonnement du client B n'existe pas :
        // resilier le bon abonnement du mauvais client est une erreur que rien
        // d'autre ne rattraperait.
        when(repo.findById(ID)).thenReturn(Optional.of(abonnementVivant()));
        UUID autreClient = UUID.randomUUID();

        assertThatThrownBy(() -> service.cancel(autreClient, ID, ACTEUR))
                .isInstanceOf(SubscriptionNotFoundException.class);

        verifyNoInteractions(activation);
        verify(repo, never()).save(any());
    }

    @Test
    void resilierUnAbonnementInconnuEstUn404() {
        when(repo.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(CLIENT, ID, ACTEUR))
                .isInstanceOf(SubscriptionNotFoundException.class)
                .hasMessageContaining(ID.toString());
    }

    // ---------- lecture ----------

    @Test
    void activeForNeRendQueLesAbonnementsVivants() {
        // La methode du depot est deja filtree ; ce banc verrouille le fait que
        // le service interroge CELLE-LA, et non un findAll qui melangerait les
        // contrats clos aux vivants et les ferait facturer.
        when(repo.findLiveByTenant(CLIENT)).thenReturn(List.of(abonnementVivant()));

        List<SubscriptionDto.View> vues = service.activeFor(CLIENT);

        assertThat(vues).hasSize(1);
        assertThat(vues.get(0).moduleCode()).isEqualTo("controlplan");
        verify(repo, never()).findAll();
    }

    // ---------- fixtures ----------

    private void clientConnu() {
        when(tenants.existsById(eq(CLIENT))).thenReturn(true);
    }

    private static Subscription abonnementVivant() {
        return Subscription.builder()
                .id(ID)
                .tenantId(CLIENT)
                .moduleCode("controlplan")
                .billingTier(STANDARD)
                .period(MONTHLY)
                .amountCents(9900)
                .currency("EUR")
                .startedOn(LocalDate.of(2026, 3, 15))
                .nextRenewal(LocalDate.of(2026, 4, 15))
                .createdAt(Instant.parse("2026-03-15T10:00:00Z"))
                .createdBy(ACTEUR)
                .build();
    }
}
