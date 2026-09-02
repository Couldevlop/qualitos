package com.openlab.qualitos.core.billing.invoice;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envoi SMTP de la facture, PDF en pièce jointe.
 *
 * <p>Même discipline que {@code SmtpAuditReminderMailer} dans le moteur de
 * qualité : le bean n'existe que si {@code qualitos.mail.enabled=true}. Sans
 * cela, aucun courriel ne part — et l'absence de bean est un échec de démarrage
 * clair plutôt qu'un envoi silencieusement avalé.
 *
 * <p><b>MIME et non message simple</b>, contrairement au rappel d'audit : une
 * facture voyage AVEC son PDF. Un lien de téléchargement obligerait le
 * comptable à s'authentifier sur la plateforme pour obtenir la pièce qu'on lui
 * réclame de payer.
 *
 * <p>Le corps reste du TEXTE BRUT : il porte la raison sociale, saisie par un
 * humain. En HTML il faudrait l'échapper, et un oubli d'échappement dans un
 * courriel se voit rarement avant qu'il ne serve (OWASP A03).
 *
 * <p>Identifiants et hôte viennent de {@code spring.mail.*}, donc de
 * l'environnement : aucun secret en dur (§18.2.3), et rien de ce qui est envoyé
 * n'est journalisé (§22-9).
 */
@Component
@ConditionalOnProperty(prefix = "qualitos.mail", name = "enabled", havingValue = "true")
public class SmtpInvoiceMailer implements InvoiceMailPort {

    private final JavaMailSender sender;
    private final String from;

    public SmtpInvoiceMailer(JavaMailSender sender,
                             @Value("${qualitos.mail.from:}") String from) {
        // Échec au DÉMARRAGE plutôt qu'au premier envoi : un expéditeur manquant
        // ferait rejeter les messages par le relais le jour de la facturation,
        // c'est-à-dire une fois par mois, dans un traitement que personne ne
        // regarde tourner. Mieux vaut un pod qui refuse de démarrer.
        if (from == null || from.isBlank()) {
            throw new IllegalStateException(
                    "qualitos.mail.enabled=true exige qualitos.mail.from (adresse d'expedition)");
        }
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void send(String to, String subject, String body, byte[] pdf) {
        MimeMessage message = sender.createMimeMessage();
        try {
            // true : message multipart, condition nécessaire à la pièce jointe.
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(singleLine(subject));
            helper.setText(body, false);
            helper.addAttachment(attachmentName(subject), new ByteArrayResource(pdf),
                    "application/pdf");
        } catch (MessagingException e) {
            // On ne rend PAS la main comme si l'envoi avait eu lieu : c'est
            // InvoiceService qui, sur cette exception, laisse la facture
            // « non envoyée » et donc renvoyable.
            throw new MailPreparationException("Preparation du courriel de facture impossible", e);
        }
        sender.send(message);
    }

    /**
     * Écrase retours chariot et sauts de ligne du sujet.
     *
     * <p>Un en-tête de courriel se termine par CRLF : y glisser un saut de
     * ligne permettrait d'en forger d'autres à la suite — un {@code Bcc:} vers
     * un tiers, par exemple — et la facture partirait où l'auteur de la chaîne
     * l'aurait décidé. Le sujet est construit à partir du numéro de facture,
     * qui est contraint ; la protection reste, parce qu'une propriété de
     * sécurité ne doit pas dépendre du format d'une chaîne voisine, que la
     * prochaine refonte pourrait changer.
     *
     * <p>Même correctif que {@code SmtpAuditReminderMailer.singleLine}.
     */
    static String singleLine(String subject) {
        return subject == null ? null : subject.replaceAll("[\\r\\n]+", " ");
    }

    /**
     * Le nom du fichier joint, réduit aux caractères sûrs d'un nom de fichier.
     *
     * <p>Un nom porteur de {@code /}, {@code \} ou {@code ..} est le début
     * d'une traversée de répertoire chez le destinataire, quand son client de
     * messagerie enregistre la pièce jointe.
     */
    static String attachmentName(String subject) {
        if (subject == null) {
            return "facture.pdf";
        }
        // Le POINT est ecarte lui aussi, alors qu'il est inoffensif dans un nom
        // de fichier : c'est ce qui fait disparaitre les « .. » d'un sujet
        // forge, plutot que de les recopier tels quels dans le nom propose au
        // client de messagerie. L'extension, elle, est ajoutee ICI, apres le
        // nettoyage — une extension issue de la saisie ne serait pas une
        // extension, ce serait une suggestion de l'appelant.
        String base = subject.replaceAll("[^A-Za-z0-9_-]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-+|-+$", "");
        return base.isBlank() ? "facture.pdf" : base + ".pdf";
    }
}
