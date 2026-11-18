package com.openlab.qualitos.core.mail;

import com.openlab.qualitos.core.partner.PartnerExceptions;
import com.openlab.qualitos.core.partner.PartnerFirmRepository;
import com.openlab.qualitos.core.secret.AesGcmSecretCipher;
import com.openlab.qualitos.core.secret.SecretCipher;
import com.openlab.qualitos.core.secret.SecretProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("La messagerie d'un cabinet partenaire")
class PartnerFirmMailSettingsServiceTest {

    private static final UUID CABINET = UUID.randomUUID();
    private static final String CLE =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

    private final SecretCipher coffre = new AesGcmSecretCipher(new SecretProperties(CLE));
    private final PartnerFirmMailSettingsRepository depot =
            mock(PartnerFirmMailSettingsRepository.class);
    private final PartnerFirmRepository cabinets = mock(PartnerFirmRepository.class);
    private final MailProbePort sonde = mock(MailProbePort.class);
    /**
     * Politique NEUTRE pour les bancs de comportement.
     *
     * <p>Les hotes de ces bancs sont fictifs. Avec la politique du SaaS, chacun
     * serait refuse faute de resoudre -- et ces bancs mesureraient alors le DNS
     * de la machine qui les joue, pas la regle qu'ils decrivent. La garde
     * elle-meme a ses bancs dedies, ci-dessous et dans MailHostPolicyTest.
     */
    private static final MailHostPolicy HOTES = new MailHostPolicy("onprem", null);

    private PartnerFirmMailSettingsService service;

    @BeforeEach
    void setUp() {
        service = new PartnerFirmMailSettingsService(
                depot, cabinets, new MailAccountAssembler(coffre, HOTES), sonde);
        given(cabinets.existsById(CABINET)).willReturn(true);
        given(depot.save(any())).willAnswer(i -> i.getArgument(0));
    }

    private static MailSettingsDto.Request requete(String motDePasse) {
        return new MailSettingsDto.Request("contact@cabinet.fr", "Cabinet Durand", null,
                "smtp.cabinet.fr", 587, MailSecurity.STARTTLS, "contact@cabinet.fr",
                motDePasse, false, true);
    }

    @Test
    @DisplayName("Le mot de passe est chiffre, comme chez un client")
    void chiffre() {
        given(depot.findById(CABINET)).willReturn(Optional.empty());

        MailSettingsDto.Response r = service.enregistrer(CABINET, requete("s3cr3t"));

        assertThat(r.hasPassword()).isTrue();
        assertThat(r.toString()).doesNotContain("s3cr3t");
    }

    @Test
    @DisplayName("Un cabinet INTROUVABLE est refuse, sur chaque chemin")
    void cabinetIntrouvable() {
        UUID inconnu = UUID.randomUUID();
        given(cabinets.existsById(inconnu)).willReturn(false);

        // Sans ce controle, une ligne de messagerie serait creee pour un
        // identifiant invente, et personne ne la verrait jamais dans un ecran.
        assertThatThrownBy(() -> service.lire(inconnu))
                .isInstanceOf(PartnerExceptions.FirmNotFound.class);
        assertThatThrownBy(() -> service.enregistrer(inconnu, requete("x")))
                .isInstanceOf(PartnerExceptions.FirmNotFound.class);
        assertThatThrownBy(() -> service.eprouver(inconnu, "a@b.fr"))
                .isInstanceOf(PartnerExceptions.FirmNotFound.class);
        assertThatThrownBy(() -> service.retirer(inconnu))
                .isInstanceOf(PartnerExceptions.FirmNotFound.class);
    }

    @Test
    @DisplayName("Un essai reussi marque la messagerie verifiee")
    void essaiReussi() {
        MailAccount compte = new MailAccount("contact@cabinet.fr", null, null, "smtp.cabinet.fr",
                587, MailSecurity.STARTTLS, "u", coffre.chiffrer("s3cr3t"), true);
        given(depot.findById(CABINET))
                .willReturn(Optional.of(new PartnerFirmMailSettings(CABINET, compte)));
        given(sonde.essayer(any(), anyString(), anyString())).willReturn(null);

        MailSettingsDto.TestResult r = service.eprouver(CABINET, "admin@cabinet.fr");

        assertThat(r.success()).isTrue();
        assertThat(r.verifiedAt()).isNotNull();
    }

    @Test
    @DisplayName("Un echec rend son motif et ne marque rien")
    void essaiEchoue() {
        MailAccount compte = new MailAccount("contact@cabinet.fr", null, null, "smtp.cabinet.fr",
                587, MailSecurity.STARTTLS, null, null, true);
        given(depot.findById(CABINET))
                .willReturn(Optional.of(new PartnerFirmMailSettings(CABINET, compte)));
        given(sonde.essayer(any(), any(), anyString())).willReturn("Authentication failed (535)");

        MailSettingsDto.TestResult r = service.eprouver(CABINET, "admin@cabinet.fr");

        assertThat(r.success()).isFalse();
        assertThat(r.detail()).contains("535");
        assertThat(r.verifiedAt()).isNull();
    }

    @Test
    @DisplayName("Eprouver une messagerie non reglee est refuse")
    void rienARegler() {
        given(depot.findById(CABINET)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.eprouver(CABINET, "admin@cabinet.fr"))
                .isInstanceOf(MailSettingsNotConfiguredException.class);
    }

    @Test
    @DisplayName("Une messagerie non activee ne sert pas a l'envoi")
    void nonActivee() {
        MailAccount compte = new MailAccount("contact@cabinet.fr", null, null, "smtp.cabinet.fr",
                587, MailSecurity.STARTTLS, "u", null, false);
        given(depot.findById(CABINET))
                .willReturn(Optional.of(new PartnerFirmMailSettings(CABINET, compte)));

        assertThat(service.pourEnvoi(CABINET)).isEmpty();
    }

    @Test
    @DisplayName("Sans reglage, la lecture le dit au lieu d'echouer")
    void sansReglage() {
        given(depot.findById(CABINET)).willReturn(Optional.empty());

        assertThat(service.lire(CABINET).configured()).isFalse();
    }
}
