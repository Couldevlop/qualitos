package com.openlab.qualitos.core.mail;

import com.openlab.qualitos.core.security.TenantContext;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * D'ou part le courriel d'une derogation, et sous quelle adresse.
 *
 * <p>Un dossier signe restait bloque pour un reglage dont l'initiateur n'avait
 * pas la main : la derogation refusait quand le client n'avait pas configure sa
 * messagerie, alors que la facture -- qui sort elle aussi vers le client, par la
 * meme methode -- se replie sur celle de la plateforme. Ce banc fixe les trois
 * etages et, surtout, la regle qui les separe : le repli ne vaut QUE pour
 * l'absence de reglage.
 */
@DisplayName("D'ou part le courriel d'une derogation")
class DerogationMailControllerExpediteurTest {

    private static final UUID CLIENT = UUID.randomUUID();
    private static final String INITIATEUR = "initiateur@organisation.fr";
    private static final String GENERIQUE = "notifications@qualitos.fr";

    private final TenantMailSettingsService settings = mock(TenantMailSettingsService.class);
    private final MailSenderFactory factory = mock(MailSenderFactory.class);
    private final MailHostPolicy hostPolicy = mock(MailHostPolicy.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<JavaMailSender> fourniture = mock(ObjectProvider.class);
    private final JavaMailSender envoyeur = mock(JavaMailSender.class);

    @BeforeEach
    void poserLeContexte() {
        TenantContext.setTenantId(CLIENT.toString());
        Jwt jeton = Jwt.withTokenValue("jeton-de-banc")
                .header("alg", "none")
                .claim("sub", UUID.randomUUID().toString())
                .claim("email", INITIATEUR)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jeton));
        given(envoyeur.createMimeMessage())
                .willAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
        given(factory.creer(any(), any())).willReturn(envoyeur);
    }

    @AfterEach
    void rendre() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private DerogationMailController controleur(boolean plateformeActive, String adresse) {
        return new DerogationMailController(settings, factory, hostPolicy, fourniture,
                plateformeActive, adresse);
    }

    private static DerogationMailController.SendRequest demande() {
        String pdf = Base64.getEncoder().encodeToString("%PDF-1.4 corps".getBytes());
        return new DerogationMailController.SendRequest("client@exemple.fr",
                "Demande de derogation DER-2026-0001", "Bonjour,", "derogation.pdf", pdf);
    }

    private static MailAccount reglageDuClient() {
        return new MailAccount("qualite@organisation.fr", "Qualite Organisation", null,
                "smtp.organisation.fr", 587, MailSecurity.STARTTLS, "u", "v1.a.b", true);
    }

    /** Le message reellement poste, pour lire ses en-tetes. */
    private MimeMessage messagePoste() throws Exception {
        ArgumentCaptor<MimeMessage> capture = ArgumentCaptor.forClass(MimeMessage.class);
        verify(envoyeur).send(capture.capture());
        return capture.getValue();
    }

    @Test
    @DisplayName("le client a regle sa messagerie : le courriel part de SON adresse")
    void laMessagerieDuClientLEmporte() throws Exception {
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.of(reglageDuClient()));
        given(hostPolicy.refus("smtp.organisation.fr")).willReturn(null);

        controleur(true, GENERIQUE).send(demande());

        assertThat(messagePoste().getFrom()[0])
                .hasToString("qualite@organisation.fr");
    }

    @Test
    @DisplayName("le client n'a rien regle : le courriel part de l'adresse generique")
    void sansReglageLaPlateformePrendLeRelais() throws Exception {
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.empty());
        given(fourniture.getIfAvailable()).willReturn(envoyeur);

        controleur(true, GENERIQUE).send(demande());

        assertThat(messagePoste().getFrom()[0]).hasToString(GENERIQUE);
    }

    @Test
    @DisplayName("l'initiateur est en copie cachee, et le client ne le voit pas")
    void lInitiateurEstEnCopieCachee() throws Exception {
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.empty());
        given(fourniture.getIfAvailable()).willReturn(envoyeur);

        controleur(true, GENERIQUE).send(demande());

        MimeMessage message = messagePoste();
        assertThat(message.getRecipients(Message.RecipientType.BCC))
                .hasSize(1);
        assertThat(message.getRecipients(Message.RecipientType.BCC)[0])
                .hasToString(INITIATEUR);
        assertThat(message.getRecipients(Message.RecipientType.TO)[0])
                .hasToString("client@exemple.fr");
        assertThat(message.getRecipients(Message.RecipientType.CC)).isNull();
    }

    @Test
    @DisplayName("la piece jointe part avec le courriel generique")
    void laPieceJointePartAussi() throws Exception {
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.empty());
        given(fourniture.getIfAvailable()).willReturn(envoyeur);

        controleur(true, GENERIQUE).send(demande());

        assertThat(messagePoste().getContent())
                .isInstanceOf(jakarta.mail.Multipart.class);
    }

    @Test
    @DisplayName("une messagerie de client dont l'hote est refuse NE se replie PAS")
    void unHoteRefuseNeSeReplitPas() {
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.of(reglageDuClient()));
        given(hostPolicy.refus("smtp.organisation.fr"))
                .willReturn("Cet hote de messagerie n'est pas autorise.");

        assertThatThrownBy(() -> controleur(true, GENERIQUE).send(demande()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("n'est pas autorise");

        verify(envoyeur, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("ni client ni plateforme : le refus dit ce que l'utilisateur peut faire")
    void sansAucuneMessagerieLeRefusEstActionnable() {
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.empty());

        assertThatThrownBy(() -> controleur(false, GENERIQUE).send(demande()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Aucune messagerie n'est disponible")
                .hasMessageContaining("votre organisation");
    }

    @Test
    @DisplayName("la plateforme active mais sans adresse d'expedition ne sert pas")
    void laPlateformeSansAdresseNeSertPas() {
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.empty());
        given(fourniture.getIfAvailable()).willReturn(envoyeur);

        assertThatThrownBy(() -> controleur(true, "  ").send(demande()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Aucune messagerie n'est disponible");
    }

    @Test
    @DisplayName("la plateforme annoncee mais absente du contexte ne sert pas non plus")
    void laPlateformeAnnonceeMaisAbsenteNeSertPas() {
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.empty());
        given(fourniture.getIfAvailable()).willReturn(null);

        assertThatThrownBy(() -> controleur(true, GENERIQUE).send(demande()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Aucune messagerie n'est disponible");
    }

    @Test
    @DisplayName("un serveur qui refuse l'envoi le dit en francais, et reste renvoyable")
    void unServeurQuiRefuseLeDitEnFrancais() {
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.empty());
        given(fourniture.getIfAvailable()).willReturn(envoyeur);
        org.mockito.BDDMockito.willThrow(new MailSendException("connexion refusee"))
                .given(envoyeur).send(any(MimeMessage.class));

        assertThatThrownBy(() -> controleur(true, GENERIQUE).send(demande()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("n'a pas accepté l'envoi")
                .hasMessageContaining("renvoyable")
                .hasMessageNotContaining("connexion refusee");
    }

    @Test
    @DisplayName("sans adresse dans le jeton, le courriel part quand meme, sans copie")
    void sansAdresseDansLeJetonLeCourrielPartSansCopie() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                Jwt.withTokenValue("sans-adresse").header("alg", "none")
                        .claim("sub", UUID.randomUUID().toString())
                        .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600))
                        .build()));
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.empty());
        given(fourniture.getIfAvailable()).willReturn(envoyeur);

        controleur(true, GENERIQUE).send(demande());

        assertThat(messagePoste().getRecipients(Message.RecipientType.BCC)).isNull();
    }

    @Test
    @DisplayName("la politique d'hote ne s'applique pas a la messagerie de la plateforme")
    void laPolitiqueDHoteNeViseQueLeClient() throws Exception {
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.empty());
        given(fourniture.getIfAvailable()).willReturn(envoyeur);

        controleur(true, GENERIQUE).send(demande());

        verify(hostPolicy, never()).refus(any());
    }
}
