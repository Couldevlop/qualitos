package com.openlab.qualitos.core.mail;

import com.openlab.qualitos.core.billing.invoice.InvoiceMailPort;
import com.openlab.qualitos.core.billing.invoice.PlatformInvoiceMailPort;
import com.openlab.qualitos.core.security.TenantContext;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import com.openlab.qualitos.mail.MailTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Le courriel part de la messagerie du client quand il en a réglé une.
 *
 * <p>C'est ce qui rend l'écran de réglage autre chose qu'un formulaire
 * décoratif. Sans ce branchement, un client aurait saisi son serveur, vu
 * « vérifiée », et ses factures seraient quand même parties de l'adresse de la
 * plateforme — une messagerie configurée et sans effet est pire que pas de
 * messagerie du tout, parce qu'elle fait croire le problème réglé.
 *
 * <p><b>Pourquoi un décorateur plutôt qu'un paramètre de plus au port.</b>
 * Ajouter un {@code tenantId} à {@code send(...)} aurait obligé chaque appelant
 * à le transmettre — et il suffit qu'un seul l'oublie pour qu'un courriel parte
 * de la mauvaise organisation. Ici le client vient du contexte de la requête,
 * le même que celui qui garde les données : il n'y a rien à ne pas oublier.
 *
 * <p><b>Le repli n'est pas silencieux au mauvais endroit.</b> Si le client n'a
 * rien réglé, on prend la messagerie de la plateforme, et c'est le
 * comportement d'avant. Mais si le client a réglé une messagerie et qu'elle
 * échoue, on ne se rabat <b>pas</b> sur celle de la plateforme : le message
 * partirait d'une adresse que le client n'a pas choisie, sous l'identité de
 * QualitOS, pour un courrier qui l'engage. L'échec remonte, et la facture
 * reste renvoyable.
 */
@Component
@Primary
public class TenantAwareInvoiceMailer implements InvoiceMailPort {

    private static final Logger log = LoggerFactory.getLogger(TenantAwareInvoiceMailer.class);

    private final PlatformInvoiceMailPort plateforme;
    private final TenantMailSettingsService settings;
    private final MailSenderFactory factory;

    public TenantAwareInvoiceMailer(PlatformInvoiceMailPort plateforme,
                                    TenantMailSettingsService settings,
                                    MailSenderFactory factory) {
        this.plateforme = plateforme;
        this.settings = settings;
        this.factory = factory;
    }

    @Override
    public void send(String to, String subject, String body, byte[] pdf) {
        Optional<MailAccount> propre = messagerieDuClient();
        if (propre.isEmpty()) {
            plateforme.send(to, subject, body, pdf);
            return;
        }
        MailAccount reglage = propre.get();
        JavaMailSender envoyeur = factory.creer(reglage, settings.motDePasseClair(reglage));
        MimeMessage message = envoyeur.createMimeMessage();
        try {
            // true : message multipart, condition necessaire a la piece jointe.
            MimeMessageHelper aide = new MimeMessageHelper(message, true, "UTF-8");
            aide.setFrom(reglage.getFromAddress());
            aide.setTo(to);
            if (reglage.getReplyTo() != null && !reglage.getReplyTo().isBlank()) {
                aide.setReplyTo(reglage.getReplyTo());
            }
            // Sujet sur UNE ligne : un saut de ligne y permettrait d'injecter
            // d'autres en-tetes, donc d'autres destinataires.
            aide.setSubject(subject == null ? "" : subject.replaceAll("[\\r\\n]+", " ").strip());
            // La facture emane de QualitOS, meme postee depuis la messagerie du
            // client : elle porte donc le logo de la plateforme.
            aide.setText(body, MailTemplate.plateforme(body));
            aide.addInline(MailTemplate.LOGO_CID,
                    new ByteArrayResource(MailTemplate.logo()), "image/png");
            aide.addAttachment(nomPieceJointe(subject), new ByteArrayResource(pdf),
                    "application/pdf");
        } catch (MessagingException e) {
            throw new IllegalStateException(
                    "Preparation du courriel impossible avec la messagerie de l'organisation.", e);
        }
        envoyeur.send(message);
        log.info("tenant.mail.sent host={} from={}", reglage.getHost(), reglage.getFromAddress());
    }

    /**
     * La messagerie du client courant, s'il y en a une d'activée.
     *
     * <p>Hors contexte de client — un traitement planifié à l'échelle de la
     * plateforme, par exemple — on rend vide, et le courrier part de la
     * messagerie de la plateforme. Deviner un client ici serait pire que de
     * n'en avoir aucun.
     */
    private Optional<MailAccount> messagerieDuClient() {
        String brut = TenantContext.getTenantId();
        if (brut == null || brut.isBlank()) {
            return Optional.empty();
        }
        try {
            return settings.pourEnvoi(UUID.fromString(brut));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private static String nomPieceJointe(String subject) {
        String base = subject == null || subject.isBlank() ? "document" : subject.strip();
        return base.replaceAll("[^A-Za-z0-9._-]", "_") + ".pdf";
    }
}
