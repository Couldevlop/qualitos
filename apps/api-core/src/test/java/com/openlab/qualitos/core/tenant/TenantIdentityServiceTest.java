package com.openlab.qualitos.core.tenant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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
    private TenantLogoRepository logos;
    private TenantIdentityService service;

    @BeforeEach
    void setUp() {
        tenants = mock(TenantRepository.class);
        identities = mock(TenantIdentityRepository.class);
        logos = mock(TenantLogoRepository.class);
        service = new TenantIdentityService(tenants, identities, logos);
        when(logos.save(any(TenantLogo.class))).thenAnswer(i -> i.getArgument(0));
        when(tenants.existsById(TENANT)).thenReturn(true);
        when(identities.save(any(TenantIdentity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    /** Un PNG minimal : signature valide, remplissage quelconque. */
    private static byte[] png() {
        byte[] octets = new byte[64];
        byte[] signature = {(byte) 0x89, 80, 78, 71, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, octets, 0, signature.length);
        return octets;
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
        assertThat(existing.getTenantId()).isEqualTo(TENANT);
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
    void leLogoEstSignaleParSaPresenceEtJamaisParSesOctets() {
        TenantIdentity existing = new TenantIdentity();
        existing.setTenantId(TENANT);
        when(identities.findById(TENANT)).thenReturn(Optional.of(existing));
        when(logos.existsById(TENANT)).thenReturn(true);

        TenantIdentityDto.Response read = service.get(TENANT);

        // Les octets ne sortent jamais par la : l'ecran apprend qu'un logo
        // existe et passe par la route dediee, que le navigateur met en cache.
        assertThat(read.logoAvailable()).isTrue();
    }

    @Test
    void unLogoPoseSansRienDAutreSAfficheQuandMeme() {
        // Le cas reel : l'administrateur televerse son logo puis quitte l'ecran
        // sans remplir le reste. Sans ce chemin, le logo semblerait perdu.
        when(identities.findById(TENANT)).thenReturn(Optional.empty());
        when(logos.existsById(TENANT)).thenReturn(true);

        assertThat(service.get(TENANT).logoAvailable()).isTrue();
    }

    @Test
    void lEnregistrementDuLogoNeTouchePasAuResteDeLIdentite() {
        TenantIdentity existing = new TenantIdentity();
        existing.setTenantId(TENANT);
        existing.setSlogan("Slogan conserve");
        when(identities.findById(TENANT)).thenReturn(Optional.of(existing));

        service.saveLogo(TENANT, "image/png", png());

        // Remplacer un logo n'ecrit pas une ligne d'identite : tables separees.
        verify(identities, never()).save(any());
        assertThat(existing.getSlogan()).isEqualTo("Slogan conserve");
    }

    @Test
    void leLogoEnregistreSonTypeNormaliseSonPoidsEtSonEmpreinte() {
        service.saveLogo(TENANT, "IMAGE/PNG; charset=binary", png());

        ArgumentCaptor<TenantLogo> capture = ArgumentCaptor.forClass(TenantLogo.class);
        verify(logos).save(capture.capture());
        TenantLogo saved = capture.getValue();
        // Le type stocke est celui qu'a VALIDE la politique, pas celui qu'a
        // annonce le client : c'est lui qui sera renvoye en Content-Type.
        assertThat(saved.getContentType()).isEqualTo("image/png");
        assertThat(saved.getSizeBytes()).isEqualTo(64);
        assertThat(saved.getSha256()).matches("^[0-9a-f]{64}$");
        assertThat(saved.getTenantId()).isEqualTo(TENANT);
    }

    @Test
    void deuxTeleversementsIdentiquesDonnentLaMemeEmpreinte() {
        // C'est ce qui permet au navigateur de ne pas reteecharger un logo
        // inchange (ETag), et de reconnaitre un envoi en double.
        assertThat(service.saveLogo(TENANT, "image/png", png()))
                .isEqualTo(service.saveLogo(TENANT, "image/png", png()));
    }

    @Test
    void unLogoRefuseParLaPolitiqueNEstPasEnregistre() {
        assertThatThrownBy(() -> service.saveLogo(TENANT, "image/svg+xml", png()))
                .isInstanceOf(LogoRejectedException.class);
        verify(logos, never()).save(any());
    }

    @Test
    void leLogoSeRelitEtSeRetire() {
        TenantLogo logo = new TenantLogo();
        logo.setTenantId(TENANT);
        when(logos.findById(TENANT)).thenReturn(Optional.of(logo));

        assertThat(service.logo(TENANT)).contains(logo);

        service.deleteLogo(TENANT);
        verify(logos).deleteById(TENANT);
        // Retirer le logo ne touche pas a l'identite : slogan et couleurs restent.
        verify(identities, never()).delete(any());
    }

    @Test
    void sansLogoLaLectureRendUnVideEtNonUneErreur() {
        when(logos.findById(TENANT)).thenReturn(Optional.empty());

        assertThat(service.logo(TENANT)).isEmpty();
    }

    @Test
    void laLectureEtLeRetraitDuLogoDUnClientInconnuSontRefuses() {
        UUID absent = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        when(tenants.existsById(absent)).thenReturn(false);

        assertThatThrownBy(() -> service.logo(absent)).isInstanceOf(TenantNotFoundException.class);
        assertThatThrownBy(() -> service.deleteLogo(absent)).isInstanceOf(TenantNotFoundException.class);
        verify(logos, never()).deleteById(any());
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
