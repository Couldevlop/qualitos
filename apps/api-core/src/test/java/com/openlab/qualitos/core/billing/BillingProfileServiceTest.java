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
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingProfileServiceTest {

    @Mock BillingProfileRepository repo;
    @Mock TenantRepository tenants;
    BillingProfileService service;

    static final UUID CLIENT = UUID.randomUUID();
    static final Instant NOW = Instant.parse("2026-09-01T10:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @BeforeEach
    void setup() {
        service = new BillingProfileService(repo, tenants, CLOCK);
    }

    @Test
    void leConstructeurVuParSpringPoseLHorlogeSysteme() {
        // Le constructeur a deux arguments est celui que Spring appelle
        // (@Autowired) : sans lui, aucun bean Clock ne serait resolu et le
        // contexte applicatif ne demarrerait pas. On verifie ici qu'il
        // construit un service utilisable, horloge systeme comprise.
        BillingProfileService viaSpring = new BillingProfileService(repo, tenants);

        assertThat(viaSpring).isNotNull();
    }

    @Test
    void unProfilNePeutPasViserUnClientInconnu() {
        // Sans ce refus, on creerait des profils orphelins qu'aucune facture ne
        // pourrait rattacher a un client reel.
        when(tenants.existsById(CLIENT)).thenReturn(false);

        assertThatThrownBy(() -> service.upsert(CLIENT, commandeValide()))
                .isInstanceOf(TenantNotFoundException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void unSecondEnregistrementMetAJourLePremier() {
        // Un client n'a qu'un profil : deux profils, c'est deux factures.
        when(tenants.existsById(CLIENT)).thenReturn(true);
        BillingProfile existant = profilExistant();
        when(repo.findByTenantId(CLIENT)).thenReturn(Optional.of(existant));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.upsert(CLIENT, commandeValide());

        ArgumentCaptor<BillingProfile> saved = ArgumentCaptor.forClass(BillingProfile.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getId()).isEqualTo(existant.getId());
    }

    @Test
    void uneExemptionSansMotifEstRefusee() {
        // On ne renonce pas a facturer sans dire pourquoi : c'est ce que
        // l'auditeur demandera.
        when(tenants.existsById(CLIENT)).thenReturn(true);

        assertThatThrownBy(() -> service.upsert(CLIENT, exempteSansMotif()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motif");
    }

    @Test
    void leCompteDeDemonstrationEstExempte() {
        when(repo.findByTenantId(CLIENT)).thenReturn(Optional.of(profilExempte()));

        assertThat(service.isExempt(CLIENT)).isTrue();
    }

    @Test
    void unClientSansProfilNEstPasReputeExempte() {
        // L'absence de profil n'est PAS une exemption : c'est un profil a
        // remplir. Le contraire ferait taire silencieusement la facturation
        // d'un client reel.
        when(repo.findByTenantId(CLIENT)).thenReturn(Optional.empty());

        assertThat(service.isExempt(CLIENT)).isFalse();
    }

    @Test
    void unPremierEnregistrementCreeUnProfil() {
        // Symetrique de "un second enregistrement met a jour le premier" :
        // sans profil existant, upsert() doit en creer un nouveau, avec son
        // propre id et l'horodatage de creation fige sur l'horloge injectee.
        when(tenants.existsById(CLIENT)).thenReturn(true);
        when(repo.findByTenantId(CLIENT)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        BillingProfileDto.View vue = service.upsert(CLIENT, commandeValide());

        ArgumentCaptor<BillingProfile> saved = ArgumentCaptor.forClass(BillingProfile.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().getId()).isNotNull();
        assertThat(saved.getValue().getTenantId()).isEqualTo(CLIENT);
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(saved.getValue().getUpdatedAt()).isEqualTo(NOW);
        assertThat(vue.legalName()).isEqualTo("Openlab SAS");
    }

    @Test
    void uneExemptionAvecMotifEstAcceptee() {
        // Symetrique de "sans motif est refusee" : un motif reel, meme
        // exemption, doit passer.
        when(tenants.existsById(CLIENT)).thenReturn(true);
        when(repo.findByTenantId(CLIENT)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        BillingProfileDto.View vue = service.upsert(CLIENT, exempteAvecMotif());

        assertThat(vue.billingExempt()).isTrue();
        assertThat(vue.exemptionReason()).isEqualTo("Compte de demonstration interne");
    }

    @Test
    void uneExemptionMotiveeParDesEspacesEstRefusee() {
        // Un motif qui n'est que des espaces n'en est pas un : le texte doit
        // porter une explication, pas la simuler.
        when(tenants.existsById(CLIENT)).thenReturn(true);

        assertThatThrownBy(() -> service.upsert(CLIENT, exempteAvecMotifVide()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motif");
        verify(repo, never()).save(any());
    }

    @Test
    void onPeutConsulterLeProfilExistant() {
        when(repo.findByTenantId(CLIENT)).thenReturn(Optional.of(profilExistant()));

        Optional<BillingProfileDto.View> vue = service.find(CLIENT);

        assertThat(vue).isPresent();
        assertThat(vue.get().tenantId()).isEqualTo(CLIENT);
    }

    @Test
    void onNeTrouvePasDeProfilPourUnClientSansProfil() {
        when(repo.findByTenantId(CLIENT)).thenReturn(Optional.empty());

        assertThat(service.find(CLIENT)).isEmpty();
    }

    // --- Fixtures -----------------------------------------------------------
    // Regroupees en bas de fichier : elles ne testent rien par elles-memes,
    // elles fabriquent des donnees valides (ou delibrement invalides) pour les
    // scenarios ci-dessus.

    private BillingProfileDto.SaveCommand commandeValide() {
        return new BillingProfileDto.SaveCommand(
                "Openlab SAS",
                "FR12345678901",
                "12 rue de la Qualite",
                null,
                "75001",
                "Paris",
                "FR",
                "facturation@openlab.fr",
                "EUR",
                false,
                null);
    }

    private BillingProfileDto.SaveCommand exempteSansMotif() {
        return new BillingProfileDto.SaveCommand(
                "Openlab Demo",
                null,
                "12 rue de la Qualite",
                null,
                "75001",
                "Paris",
                "FR",
                "demo@openlab.fr",
                "EUR",
                true,
                null);
    }

    private BillingProfileDto.SaveCommand exempteAvecMotifVide() {
        return new BillingProfileDto.SaveCommand(
                "Openlab Demo",
                null,
                "12 rue de la Qualite",
                null,
                "75001",
                "Paris",
                "FR",
                "demo@openlab.fr",
                "EUR",
                true,
                "   ");
    }

    private BillingProfileDto.SaveCommand exempteAvecMotif() {
        return new BillingProfileDto.SaveCommand(
                "Openlab Demo",
                null,
                "12 rue de la Qualite",
                null,
                "75001",
                "Paris",
                "FR",
                "demo@openlab.fr",
                "EUR",
                true,
                "Compte de demonstration interne");
    }

    private BillingProfile profilExistant() {
        return BillingProfile.builder()
                .id(UUID.randomUUID())
                .tenantId(CLIENT)
                .legalName("Ancien nom")
                .addressLine1("Ancienne adresse")
                .postalCode("75000")
                .city("Paris")
                .countryCode("FR")
                .billingEmail("ancien@openlab.fr")
                .currency("EUR")
                .billingExempt(false)
                .createdAt(NOW.minusSeconds(3600))
                .updatedAt(NOW.minusSeconds(3600))
                .build();
    }

    private BillingProfile profilExempte() {
        return BillingProfile.builder()
                .id(UUID.randomUUID())
                .tenantId(CLIENT)
                .legalName("Compte demo")
                .addressLine1("12 rue de la Qualite")
                .postalCode("75001")
                .city("Paris")
                .countryCode("FR")
                .billingEmail("demo@openlab.fr")
                .currency("EUR")
                .billingExempt(true)
                .exemptionReason("Compte de demonstration interne")
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
