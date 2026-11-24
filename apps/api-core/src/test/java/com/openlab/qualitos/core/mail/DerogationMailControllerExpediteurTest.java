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
 * <p>De la messagerie de l'organisation, et d'aucune autre. Le reglage vit dans
 * les parametres de cette organisation, comme tout parametrage (ADR 0127), et
 * elle y designe une adresse generique -- celle du service qualite, non celle
 * d'une personne. Pas de repli sur une messagerie de plateforme : le courriel
 * engage l'organisation vis-a-vis de son client, et il doit partir d'une adresse
 * que ce client reconnait.
 */
@DisplayName("D'ou part le courriel d'une derogation")
class DerogationMailControllerExpediteurTest {

    private static final UUID CLIENT = UUID.randomUUID();
    private static final String INITIATEUR = "initiateur@organisation.fr";

    private final TenantMailSettingsService settings = mock(TenantMailSettingsService.class);
    private final MailSenderFactory factory = mock(MailSenderFactory.class);
    private final MailHostPolicy hostPolicy = mock(MailHostPolicy.class);
    private final JavaMailSender envoyeur = mock(JavaMailSender.class);

    private DerogationMailController controleur;

    @BeforeEach
    void poserLeContexte() {
        TenantContext.setTenantId(CLIENT.toString());
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jeton(INITIATEUR)));
        given(envoyeur.createMimeMessage())
                .willAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
        given(factory.creer(any(), any())).willReturn(envoyeur);
        controleur = new DerogationMailController(settings, factory, hostPolicy);
    }

    @AfterEach
    void rendre() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private static Jwt jeton(String adresse) {
        Jwt.Builder construction = Jwt.withTokenValue("jeton-de-banc")
                .header("alg", "none")
                .claim("sub", UUID.randomUUID().toString())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600));
        if (adresse != null) {
            construction.claim("email", adresse);
        }
        return construction.build();
    }

    private static DerogationMailController.SendRequest demande() {
        String pdf = Base64.getEncoder().encodeToString("%PDF-1.4 corps".getBytes());
        return new DerogationMailController.SendRequest("client@exemple.fr",
                "Demande de derogation DER-2026-0001", "Bonjour,", "derogation.pdf", pdf);
    }

    private static MailAccount reglageDuClient() {
        return new MailAccount("qualite@organisation.fr", "Qualite Organisation",
                "qualite@organisation.fr", "smtp.organisation.fr", 587,
                MailSecurity.STARTTLS, "u", "v1.a.b", true);
    }

    /** Le reglage de l'organisation est en place et son hote accepte. */
    private void messagerieReglee() {
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.of(reglageDuClient()));
        given(hostPolicy.refus("smtp.organisation.fr")).willReturn(null);
    }

    /** Le message reellement poste, pour lire ses en-tetes. */
    private MimeMessage messagePoste() throws Exception {
        ArgumentCaptor<MimeMessage> capture = ArgumentCaptor.forClass(MimeMessage.class);
        verify(envoyeur).send(capture.capture());
        return capture.getValue();
    }

    @Test
    @DisplayName("le courriel part de l'adresse reglee par l'organisation")
    void leCourrielPartDeLAdresseDeLOrganisation() throws Exception {
        messagerieReglee();

        controleur.send(demande());

        assertThat(messagePoste().getFrom()[0]).hasToString("qualite@organisation.fr");
    }

    @Test
    @DisplayName("l'initiateur est en copie cachee, et le client ne le voit pas")
    void lInitiateurEstEnCopieCachee() throws Exception {
        messagerieReglee();

        controleur.send(demande());

        MimeMessage message = messagePoste();
        assertThat(message.getRecipients(Message.RecipientType.BCC)).hasSize(1);
        assertThat(message.getRecipients(Message.RecipientType.BCC)[0])
                .hasToString(INITIATEUR);
        assertThat(message.getRecipients(Message.RecipientType.TO)[0])
                .hasToString("client@exemple.fr");
        // La copie n'a de sens que cachee : en Cc, l'adresse interne de
        // l'initiateur arriverait sous les yeux du client.
        assertThat(message.getRecipients(Message.RecipientType.CC)).isNull();
    }

    @Test
    @DisplayName("la reponse du client revient a l'adresse que l'organisation a designee")
    void laReponseRevientALAdresseDesignee() throws Exception {
        messagerieReglee();

        controleur.send(demande());

        assertThat(messagePoste().getReplyTo()[0]).hasToString("qualite@organisation.fr");
    }

    @Test
    @DisplayName("la derogation part en piece jointe")
    void laDerogationPartEnPieceJointe() throws Exception {
        messagerieReglee();

        controleur.send(demande());

        assertThat(messagePoste().getContent()).isInstanceOf(jakarta.mail.Multipart.class);
    }

    @Test
    @DisplayName("sans messagerie reglee, le refus nomme le reglage a poser")
    void sansMessagerieRegleeLeRefusNommeLeReglage() {
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.empty());

        assertThatThrownBy(() -> controleur.send(demande()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("La messagerie de votre organisation n'est pas configurée");

        verify(envoyeur, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("aucun repli : sans reglage, rien ne part d'une autre adresse")
    void aucunRepliSurUneAutreAdresse() {
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.empty());

        assertThatThrownBy(() -> controleur.send(demande()))
                .isInstanceOf(ResponseStatusException.class);

        // Pas de fabrique d'expediteur appelee, donc aucune messagerie de
        // substitution : le courriel ne peut pas partir sous une identite que
        // l'organisation n'a pas choisie.
        verify(factory, never()).creer(any(), any());
    }

    @Test
    @DisplayName("un hote refuse par la politique arrete l'envoi")
    void unHoteRefuseArreteLEnvoi() {
        given(settings.pourEnvoi(CLIENT)).willReturn(Optional.of(reglageDuClient()));
        given(hostPolicy.refus("smtp.organisation.fr"))
                .willReturn("Ce serveur n'est pas joignable depuis la plateforme.");

        assertThatThrownBy(() -> controleur.send(demande()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("n'est pas joignable");

        verify(envoyeur, never()).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("un serveur qui refuse l'envoi le dit en francais, et reste renvoyable")
    void unServeurQuiRefuseLeDitEnFrancais() {
        messagerieReglee();
        org.mockito.BDDMockito.willThrow(new MailSendException("connexion refusee"))
                .given(envoyeur).send(any(MimeMessage.class));

        assertThatThrownBy(() -> controleur.send(demande()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("n'a pas accepté l'envoi")
                .hasMessageContaining("renvoyable")
                // La cause technique reste au journal (OWASP A09).
                .hasMessageNotContaining("connexion refusee");
    }

    @Test
    @DisplayName("sans adresse dans le jeton, le courriel part quand meme, sans copie")
    void sansAdresseDansLeJetonLeCourrielPartSansCopie() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jeton(null)));
        messagerieReglee();

        controleur.send(demande());

        assertThat(messagePoste().getRecipients(Message.RecipientType.BCC)).isNull();
    }

    @Test
    @DisplayName("l en-tete porte le NOM de l organisation, et pas le logo QualitOS")
    void lEnTetePorteLeNomDeLOrganisation() throws Exception {
        messagerieReglee();

        controleur.send(demande());

        String entier = contenuEntier(messagePoste());
        assertThat(entier).contains("Qualite Organisation");
        // Le fond de l affaire : ce courriel part vers le client d un client,
        // sous une adresse que ce client a reglee, et il l engage. Le logo de la
        // plateforme tromperait le destinataire sur qui lui ecrit.
        assertThat(entier).doesNotContain("logo-qualitos");
    }

    @Test
    @DisplayName("les deux versions partent : le texte reste entier sous le HTML")
    void lesDeuxVersionsPartent() throws Exception {
        messagerieReglee();

        controleur.send(demande());

        String entier = contenuEntier(messagePoste());
        assertThat(entier).contains("Bonjour,");
        assertThat(entier).contains("<!DOCTYPE html>");
    }

    /** Le message entier, tel qu il partirait sur le reseau. */
    private static String contenuEntier(MimeMessage message) throws Exception {
        java.io.ByteArrayOutputStream sortie = new java.io.ByteArrayOutputStream();
        message.writeTo(sortie);
        // Le corps voyage encode : on retire les coupures de ligne souples, sans
        // quoi une balise se lirait coupee en deux.
        return sortie.toString(java.nio.charset.StandardCharsets.UTF_8)
                .replace("=\r\n", "").replace("=\n", "");
    }
}
