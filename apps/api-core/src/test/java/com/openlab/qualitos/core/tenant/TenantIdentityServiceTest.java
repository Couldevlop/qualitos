package com.openlab.qualitos.core.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * L'identité d'un client : ce qui habille son espace et ce qui figure sur ses
 * documents. Le service garde deux choses : qu'on ne décore pas un client qui
 * n'existe pas, et qu'on n'enregistre pas une apparence illisible.
 */
class TenantIdentityServiceTest {

    private static final UUID TENANT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private TenantRepository tenants;
    private TenantIdentityRepository identities;
    private TenantIdentityService service;

    @BeforeEach
    void setUp() {
        tenants = mock(TenantRepository.class);
        identities = mock(TenantIdentityRepository.class);
        service = new TenantIdentityService(tenants, identities);
        when(tenants.existsById(TENANT)).thenReturn(true);
        when(identities.save(any(TenantIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private static TenantIdentityDto.Request request() {
        return new TenantIdentityDto.Request(
                "La qualité, mesurée", "Clinique du Parc SA", "SA", "552100554", "FR40552100554",
                "12 rue des Lilas", null, "34000", "Montpellier", "FR",
                "qualite@clinique-parc.example", "+33467000000",
                "#0B6B5C", "#1D5FA8", "AIRY_LIGHT");
    }

    @Test
    void enregistreLIdentiteEtLaRelit() {
        TenantIdentityDto.Response saved = service.save(TENANT, request());

        assertThat(saved.slogan()).isEqualTo("La qualité, mesurée");
        assertThat(saved.legalName()).isEqualTo("Clinique du Parc SA");
        assertThat(saved.registrationNumber()).isEqualTo("552100554");
        assertThat(saved.primaryColor()).isEqualTo("#0B6B5C");
        assertThat(saved.secondaryColor()).isEqualTo("#1D5FA8");
        assertThat(saved.themeCode()).isEqualTo(TenantTheme.AIRY_LIGHT);
        assertThat(saved.city()).isEqualTo("Montpellier");
    }

    @Test
    void secondEnregistrementMetAJourLaMemeLigne() {
        TenantIdentity existing = new TenantIdentity();
        existing.setTenantId(TENANT);
        existing.setSlogan("Ancien slogan");
        when(identities.findById(TENANT)).thenReturn(Optional.of(existing));

        service.save(TENANT, request());

        // La clé primaire EST le tenant : un client a une identité, pas une liste.
        assertThat(existing.getSlogan()).isEqualTo("La qualité, mesurée");
        verify(identities).save(existing);
    }

    @Test
    void couleurPrincipaleIllisibleRefusee() {
        TenantIdentityDto.Request pale = new TenantIdentityDto.Request(
                null, "Clinique", null, null, null, null, null, null, null, null, null, null,
                "#EEEEEE", null, "AIRY_LIGHT");

        assertThatThrownBy(() -> service.save(TENANT, pale))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contraste");
        verify(identities, never()).save(any());
    }

    @Test
    void couleurSecondaireIllisibleRefuseeAussi() {
        TenantIdentityDto.Request pale = new TenantIdentityDto.Request(
                null, "Clinique", null, null, null, null, null, null, null, null, null, null,
                "#0B6B5C", "#FAFAFA", "AIRY_LIGHT");

        assertThatThrownBy(() -> service.save(TENANT, pale))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void surLeThemeSombreLeContrasteSeMesureSurLeFondSombre() {
        // La même couleur peut être illisible sur blanc et parfaite sur noir :
        // mesurer toujours sur blanc interdirait les thèmes sombres.
        TenantIdentityDto.Request clair = new TenantIdentityDto.Request(
                null, "Clinique", null, null, null, null, null, null, null, null, null, null,
                "#4CC3AD", null, "DARK");

        TenantIdentityDto.Response saved = service.save(TENANT, clair);

        assertThat(saved.primaryColor()).isEqualTo("#4CC3AD");
    }

    @Test
    void sansCouleurLIdentiteResteValide() {
        TenantIdentityDto.Request sansCouleur = new TenantIdentityDto.Request(
                "Slogan", "Clinique", null, null, null, null, null, null, null, null, null, null,
                null, null, "DENSE_LIGHT");

        TenantIdentityDto.Response saved = service.save(TENANT, sansCouleur);

        assertThat(saved.primaryColor()).isNull();
        assertThat(saved.themeCode()).isEqualTo(TenantTheme.DENSE_LIGHT);
    }

    @Test
    void themeInconnuRefuse() {
        TenantIdentityDto.Request neon = new TenantIdentityDto.Request(
                null, "Clinique", null, null, null, null, null, null, null, null, null, null,
                null, null, "NEON");

        assertThatThrownBy(() -> service.save(TENANT, neon))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("thème");
    }

    @Test
    void clientInconnuRefuse() {
        UUID absent = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        when(tenants.existsById(absent)).thenReturn(false);

        assertThatThrownBy(() -> service.save(absent, request()))
                .isInstanceOf(TenantNotFoundException.class);
        verify(identities, never()).save(any());
    }

    @Test
    void lectureDUnClientSansIdentiteRendLIdentiteDuSocle() {
        when(identities.findById(TENANT)).thenReturn(Optional.empty());

        TenantIdentityDto.Response read = service.get(TENANT);

        // Pas une erreur : un client fraîchement créé n'a pas encore d'identité,
        // et l'application doit s'ouvrir quand même.
        assertThat(read.themeCode()).isEqualTo(TenantTheme.AIRY_LIGHT);
        assertThat(read.slogan()).isNull();
        assertThat(read.logoAvailable()).isFalse();
    }

    @Test
    void lectureDUnClientInconnuRefusee() {
        UUID absent = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        when(tenants.existsById(absent)).thenReturn(false);

        assertThatThrownBy(() -> service.get(absent)).isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void leLogoEstSignaleParSaPresenceEtJamaisParSonChemin() {
        TenantIdentity existing = new TenantIdentity();
        existing.setTenantId(TENANT);
        existing.setLogoObjectKey("tenants/a1/logo.png");
        existing.setLogoContentType("image/png");
        when(identities.findById(TENANT)).thenReturn(Optional.of(existing));

        TenantIdentityDto.Response read = service.get(TENANT);

        // Le chemin de stockage ne sort jamais : l'écran passe par la route du
        // logo, pas par l'objet.
        assertThat(read.logoAvailable()).isTrue();
    }

    @Test
    void lEnregistrementDuLogoNeTouchePasAuResteDeLIdentite() {
        TenantIdentity existing = new TenantIdentity();
        existing.setTenantId(TENANT);
        existing.setSlogan("Slogan conservé");
        when(identities.findById(TENANT)).thenReturn(Optional.of(existing));

        service.saveLogo(TENANT, "tenants/a1/logo.webp", "image/webp");

        assertThat(existing.getLogoObjectKey()).isEqualTo("tenants/a1/logo.webp");
        assertThat(existing.getLogoContentType()).isEqualTo("image/webp");
        assertThat(existing.getSlogan()).isEqualTo("Slogan conservé");
    }

    @Test
    void leLogoDUnClientInconnuEstRefuse() {
        UUID absent = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        when(tenants.existsById(absent)).thenReturn(false);

        assertThatThrownBy(() -> service.saveLogo(absent, "k", "image/png"))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void codeDeThemeAbsentOuVideRetombeSurLeDefaut() {
        // Un client qui n'a rien choisi n'est pas un client en erreur.
        assertThat(TenantTheme.of(null)).isEqualTo(TenantTheme.AIRY_LIGHT);
        assertThat(TenantTheme.of("  ")).isEqualTo(TenantTheme.AIRY_LIGHT);
    }

    @Test
    void chaqueThemeLivreSeRelitParSonCode() {
        for (TenantTheme theme : TenantTheme.values()) {
            assertThat(TenantTheme.of(theme.name())).isEqualTo(theme);
        }
    }

    @Test
    void laCleDeLObjetEtSonTypeSeLisentPourServirLeFichier() {
        TenantIdentity existing = new TenantIdentity();
        existing.setTenantId(TENANT);
        existing.setLogoObjectKey("tenants/a1/logo.png");
        existing.setLogoContentType("image/png");
        when(identities.findById(TENANT)).thenReturn(Optional.of(existing));

        assertThat(service.logoObjectKey(TENANT)).isEqualTo("tenants/a1/logo.png");
        assertThat(service.logoContentType(TENANT)).isEqualTo("image/png");
    }

    @Test
    void sansLogoEnregistreLaCleEstAbsenteSansErreur() {
        when(identities.findById(TENANT)).thenReturn(Optional.empty());

        assertThat(service.logoObjectKey(TENANT)).isNull();
        assertThat(service.logoContentType(TENANT)).isNull();
    }

    @Test
    void laCleDUnLogoDeClientInconnuEstRefusee() {
        UUID absent = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        when(tenants.existsById(absent)).thenReturn(false);

        assertThatThrownBy(() -> service.logoObjectKey(absent))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void lIdentiteRelitChacunDeSesChampsDAdresseEtDeContact() {
        // Les mentions legales figurent sur les rapports signes : une colonne
        // ecrite mais jamais relue est un champ perdu a la premiere impression.
        TenantIdentityDto.Request complet = new TenantIdentityDto.Request(
                "Slogan", "Raison sociale", "SAS", "123456789", "FR00123456789",
                "1 rue A", "Batiment B", "75001", "Paris", "FR",
                "contact@exemple.test", "+33100000000", null, null, "HIGH_CONTRAST");

        TenantIdentityDto.Response saved = service.save(TENANT, complet);

        assertThat(saved.legalForm()).isEqualTo("SAS");
        assertThat(saved.vatNumber()).isEqualTo("FR00123456789");
        assertThat(saved.addressLine1()).isEqualTo("1 rue A");
        assertThat(saved.addressLine2()).isEqualTo("Batiment B");
        assertThat(saved.postalCode()).isEqualTo("75001");
        assertThat(saved.countryCode()).isEqualTo("FR");
        assertThat(saved.contactEmail()).isEqualTo("contact@exemple.test");
        assertThat(saved.contactPhone()).isEqualTo("+33100000000");
        assertThat(saved.themeCode()).isEqualTo(TenantTheme.HIGH_CONTRAST);
    }

    @Test
    void lesQuatreThemesLivresSontExposesEtPortentLeurFond() {
        assertThat(TenantTheme.values()).hasSize(4);
        assertThat(TenantTheme.AIRY_LIGHT.background()).isEqualTo("#FFFFFF");
        assertThat(TenantTheme.DARK.background()).isEqualTo("#11181B");
    }
}
