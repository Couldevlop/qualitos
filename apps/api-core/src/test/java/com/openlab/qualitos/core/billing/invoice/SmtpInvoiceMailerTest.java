package com.openlab.qualitos.core.billing.invoice;

import jakarta.mail.Multipart;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmtpInvoiceMailer")
class SmtpInvoiceMailerTest {

    @Mock JavaMailSender sender;

    SmtpInvoiceMailer mailer;

    @BeforeEach
    void setup() {
        mailer = new SmtpInvoiceMailer(sender, "facturation@qualitos.test");
    }

    /**
     * Un vrai {@link MimeMessage}, fabrique par l'implementation Spring : c'est
     * le seul moyen de relire ce qui a reellement ete assemble — destinataire,
     * sujet, corps, piece jointe — plutot que de verifier qu'on a appele des
     * setters.
     *
     * <p>Appele par les seuls bancs qui envoient. Le poser dans {@code setup()}
     * en ferait un stub inutile pour les bancs de nom de fichier et de
     * configuration, que la strictesse de Mockito signale a juste titre.
     */
    private void unVraiMessageEstFabrique() {
        when(sender.createMimeMessage())
                .thenAnswer(invocation -> new JavaMailSenderImpl().createMimeMessage());
    }

    @Test
    void leCourrielPartDeLAdresseConfigureeVersLeDestinataireDeFacturation() throws Exception {
        unVraiMessageEstFabrique();
        mailer.send("compta@acme.example", "Facture FA-2026-0007 - 2026-09", "Bonjour", pdf());

        MimeMessage envoye = capture();
        assertThat(envoye.getFrom()[0].toString()).isEqualTo("facturation@qualitos.test");
        assertThat(envoye.getAllRecipients()[0].toString()).isEqualTo("compta@acme.example");
        assertThat(envoye.getSubject()).isEqualTo("Facture FA-2026-0007 - 2026-09");
    }

    @Test
    void lePdfVoyageEnPieceJointe() throws Exception {
        unVraiMessageEstFabrique();
        // Un lien de telechargement obligerait le comptable a s'authentifier sur
        // la plateforme pour obtenir la piece qu'on lui reclame de payer.
        mailer.send("compta@acme.example", "Facture FA-2026-0007", "Bonjour", pdf());

        assertThat(partNames(capture())).contains("Facture-FA-2026-0007.pdf");
    }

    @Test
    void unDestinataireIllisibleFaitEchouerLEnvoiAuLieuDeLeSimuler() {
        // Une adresse que JavaMail refuse d'analyser leve une MessagingException
        // a l'ASSEMBLAGE, avant tout envoi. Rendre la main comme si tout allait
        // bien laisserait InvoiceService marquer la facture « envoyee » alors
        // qu'elle n'est jamais partie — le client ne la recevrait pas, et la
        // plateforme affirmerait le contraire.
        unVraiMessageEstFabrique();

        assertThatThrownBy(() -> mailer.send("pas@une@adresse", "Facture", "Bonjour", pdf()))
                .isInstanceOf(MailPreparationException.class);

        verify(sender, never()).send(any(MimeMessage.class));
    }

    // ---------- injection d'en-tetes ----------

    @Test
    void unSautDeLigneDansLeSujetNePermetPasDeForgerUnEnTete() {
        // Un en-tete de courriel se termine par CRLF : y glisser un saut de
        // ligne permettrait d'en forger d'autres a la suite — un Bcc: vers un
        // tiers — et la facture partirait ou l'auteur de la chaine l'aurait
        // decide.
        assertThat(SmtpInvoiceMailer.singleLine("Facture\r\nBcc: espion@ailleurs.test"))
                .isEqualTo("Facture Bcc: espion@ailleurs.test");
        assertThat(SmtpInvoiceMailer.singleLine("Facture\nsuite")).isEqualTo("Facture suite");
        assertThat(SmtpInvoiceMailer.singleLine(null)).isNull();
    }

    @Test
    void leNomDeLaPieceJointeNePeutPasRemonterLArborescence() {
        // Un nom porteur de « / » ou « .. » est le debut d'une traversee de
        // repertoire chez le destinataire, quand son client de messagerie
        // enregistre la piece jointe.
        assertThat(SmtpInvoiceMailer.attachmentName("../../etc/passwd"))
                .isEqualTo("etc-passwd.pdf")
                .doesNotContain("/")
                .doesNotContain("..");
        assertThat(SmtpInvoiceMailer.attachmentName("Facture FA-2026-0007"))
                .isEqualTo("Facture-FA-2026-0007.pdf");
        assertThat(SmtpInvoiceMailer.attachmentName(null)).isEqualTo("facture.pdf");
        assertThat(SmtpInvoiceMailer.attachmentName("   ")).isEqualTo("facture.pdf");
    }

    // ---------- configuration ----------

    @Test
    void sansAdresseDExpeditionLeBeanRefuseDeDemarrer() {
        // Echec au DEMARRAGE plutot qu'au premier envoi : un expediteur manquant
        // ferait rejeter les messages par le relais le jour de la facturation,
        // c'est-a-dire une fois par mois, dans un traitement que personne ne
        // regarde tourner.
        assertThatThrownBy(() -> new SmtpInvoiceMailer(sender, "  "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("qualitos.mail.from");
        assertThatThrownBy(() -> new SmtpInvoiceMailer(sender, null))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(sender);
    }

    // ---------- montage ----------

    private MimeMessage capture() {
        ArgumentCaptor<MimeMessage> message = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(message.capture());
        return message.getValue();
    }

    private static List<String> partNames(MimeMessage message) throws Exception {
        Multipart multipart = (Multipart) message.getContent();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < multipart.getCount(); i++) {
            String name = multipart.getBodyPart(i).getFileName();
            if (name != null) {
                names.add(name);
            }
        }
        return names;
    }

    private static byte[] pdf() {
        return "%PDF-1.4 fake".getBytes();
    }
}
