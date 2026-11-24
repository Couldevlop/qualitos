package com.openlab.qualitos.quality.audit;

import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpAuditReminderMailerTest {

    @Mock JavaMailSender sender;

    @BeforeEach
    void donneAuFauxEnvoyeurDeQuoiFabriquerUnMessage() {
        lenient().when(sender.createMimeMessage())
                .thenAnswer(i -> new MimeMessage(Session.getInstance(new Properties())));
    }

    /** Le message reellement remis a l'envoyeur. */
    private MimeMessage messageRemis() {
        ArgumentCaptor<MimeMessage> capture = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(capture.capture());
        return capture.getValue();
    }

    @Test
    void sendsAMessageFromTheConfiguredAddress() throws Exception {
        SmtpAuditReminderMailer mailer = new SmtpAuditReminderMailer(sender, props("qms@exemple.test"));

        mailer.send("pilote@exemple.test", "Audit à préparer : X", "Corps du rappel");

        MimeMessage m = messageRemis();
        assertThat(m.getFrom()[0]).hasToString("qms@exemple.test");
        assertThat(m.getRecipients(Message.RecipientType.TO)[0])
                .hasToString("pilote@exemple.test");
        assertThat(m.getSubject()).isEqualTo("Audit à préparer : X");
    }

    @Test
    void sendsBothAPlainTextAndAnHtmlPart() throws Exception {
        // Les deux versions, et le texte brut reste entier : un lecteur qui
        // refuse le HTML garde un message complet, pas une page vide.
        SmtpAuditReminderMailer mailer = new SmtpAuditReminderMailer(sender, props("qms@exemple.test"));

        mailer.send("pilote@exemple.test", "Audit", "Corps du rappel");

        String brut = contenuEntier(messageRemis());
        assertThat(brut).contains("Corps du rappel");
        assertThat(brut).contains("<!DOCTYPE html>");
        // L'en-tete de la plateforme : ce rappel vient de QualitOS.
        assertThat(brut).contains("logo-qualitos");
    }

    @Test
    void escapesTheAuditTitleInTheHtmlPart() throws Exception {
        // Le corps porte le titre de l'audit, saisi par un utilisateur. C'etait
        // le motif du texte brut seul ; l'echappement vit desormais dans le
        // gabarit, eprouve a un seul endroit.
        SmtpAuditReminderMailer mailer = new SmtpAuditReminderMailer(sender, props("qms@exemple.test"));

        mailer.send("a@b.test", "Audit", "Titre <script>alert(1)</script>");

        // La version TEXTE porte legitimement les caracteres bruts : en
        // text/plain ils n ouvrent rien. C est la version HTML qui doit les
        // avoir echappes, et la forme echappee ne peut venir que d elle.
        assertThat(contenuEntier(messageRemis())).contains("&lt;script&gt;");
    }

    @Test
    void sendsAMessageEvenWithoutABody() {
        SmtpAuditReminderMailer mailer = new SmtpAuditReminderMailer(sender, props("qms@exemple.test"));

        mailer.send("a@b.test", "Audit", null);

        verify(sender).send(any(MimeMessage.class));
    }

    /** Le message entier, en-tetes et parties, tel qu'il partirait sur le reseau. */
    private static String contenuEntier(MimeMessage message) throws Exception {
        java.io.ByteArrayOutputStream sortie = new java.io.ByteArrayOutputStream();
        message.writeTo(sortie);
        // Le corps voyage encode (quoted-printable) : on retire les coupures de
        // ligne souples, sans quoi une balise se lirait coupee en deux.
        return sortie.toString(java.nio.charset.StandardCharsets.UTF_8)
                .replace("=\r\n", "").replace("=\n", "");
    }

    @Test
    void refusesToStartWithoutASenderAddress() {
        // Échouer au démarrage, pas au premier envoi : un expéditeur manquant se
        // découvrirait sinon un mois plus tard, dans un ordonnanceur muet.
        assertThatThrownBy(() -> new SmtpAuditReminderMailer(sender, props(null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("qualitos.mail.from");
        assertThatThrownBy(() -> new SmtpAuditReminderMailer(sender, props("   ")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void letsSmtpFailuresPropagate_soTheCallerCanDecide() {
        SmtpAuditReminderMailer mailer = new SmtpAuditReminderMailer(sender, props("qms@exemple.test"));
        doThrow(new MailSendException("relais injoignable")).when(sender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> mailer.send("a@b.test", "s", "b"))
                .isInstanceOf(MailSendException.class);
    }

    @Test
    void isGatedOffByDefault_likeObjectStorageAndKafka() {
        ConditionalOnProperty gate = SmtpAuditReminderMailer.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(gate).isNotNull();
        assertThat(gate.prefix()).isEqualTo("qualitos.mail");
        assertThat(gate.name()).containsExactly("enabled");
        assertThat(gate.havingValue()).isEqualTo("true");
        // Pas de matchIfMissing : sans la propriété, aucun bean — l'application
        // démarre et le rappel reste interne.
        assertThat(gate.matchIfMissing()).isFalse();
    }

    // --- injection d'en-têtes par le sujet (OWASP A03) --------------------------

    @Test
    void subject_neverCarriesALineBreakToTheHeader() {
        // Le sujet porte le titre de l'audit, saisi par un utilisateur. Un en-tête
        // se termine par CRLF : un saut de ligne laissé passer permettrait d'en
        // forger d'autres — un Bcc: vers un tiers — et le rappel partirait où
        // l'auteur du titre l'aurait décidé.
        //
        // Aujourd'hui JavaMail encoderait de toute façon le sujet en RFC 2047, à
        // cause des accents du préfixe. C'est un accident de rédaction, pas une
        // garantie : ce test tient la garantie, quelle que soit la typographie.
        assertThat(SmtpAuditReminderMailer.singleLine("Audit\r\nBcc: tiers@ailleurs.test"))
                .isEqualTo("Audit Bcc: tiers@ailleurs.test");
        assertThat(SmtpAuditReminderMailer.singleLine("a\nb\rc")).isEqualTo("a b c");
    }

    @Test
    void subject_withoutLineBreak_isLeftIntact() {
        assertThat(SmtpAuditReminderMailer.singleLine("Audit à préparer : presse 4"))
                .isEqualTo("Audit à préparer : presse 4");
        assertThat(SmtpAuditReminderMailer.singleLine(null)).isNull();
    }

    @Test
    void send_stripsLineBreaksBeforeHandingTheMessageOver() throws Exception {
        SmtpAuditReminderMailer mailer = new SmtpAuditReminderMailer(sender, props("qms@exemple.test"));

        mailer.send("a@b.test", "Audit\r\nBcc: tiers@ailleurs.test", "corps");

        assertThat(messageRemis().getSubject()).doesNotContain("\r").doesNotContain("\n");
    }

    private AuditMailProperties props(String from) {
        AuditMailProperties p = new AuditMailProperties();
        p.setEnabled(true);
        p.setFrom(from);
        return p;
    }
}
