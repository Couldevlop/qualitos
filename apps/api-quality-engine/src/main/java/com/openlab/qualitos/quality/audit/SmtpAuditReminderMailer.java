package com.openlab.qualitos.quality.audit;

import com.openlab.qualitos.mail.MailTemplate;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Envoi SMTP du rappel d'échéance (§4.4), sur le modèle exact de l'adaptateur S3 :
 * activé uniquement quand {@code qualitos.mail.enabled=true}. Sans cela, aucun bean
 * n'est créé et le rappel se limite à la notification interne.
 *
 * <p>Message en DEUX VERSIONS, texte et HTML. Le texte brut reste envoyé tel
 * quel : un lecteur qui refuse le HTML garde un message complet.
 *
 * <p>Ce courriel était en texte brut seul, et le motif écrit ici était bon : le
 * corps porte le titre de l'audit, saisi par un utilisateur, et un oubli
 * d'échappement dans un courriel se voit rarement avant qu'il ne serve
 * (OWASP A03). Ce qui a changé n'est pas le risque, c'est OÙ il se traite :
 * l'échappement ne se refait plus à chaque envoi, il vit une seule fois dans
 * {@link MailTemplate}, où il est éprouvé par des bancs dédiés. Un oubli n'est
 * plus possible call site par call site.
 *
 * <p>Identifiants et hôte viennent de {@code spring.mail.*}, donc de
 * l'environnement : aucun secret en dur (§18.2.3), et rien de ce qui est envoyé
 * n'est journalisé (§22-9).
 */
@Component
@ConditionalOnProperty(prefix = "qualitos.mail", name = "enabled", havingValue = "true")
public class SmtpAuditReminderMailer implements AuditReminderMailer {

    private final JavaMailSender sender;
    private final String from;

    public SmtpAuditReminderMailer(JavaMailSender sender, AuditMailProperties props) {
        // Échec au DÉMARRAGE plutôt qu'au premier envoi : un expéditeur manquant
        // ferait rejeter les messages par le relais un mois plus tard, dans un
        // ordonnanceur que personne ne regarde. Mieux vaut un pod qui refuse de
        // démarrer qu'un dispositif de rappel qui se croit actif.
        if (props.getFrom() == null || props.getFrom().isBlank()) {
            throw new IllegalStateException(
                    "qualitos.mail.enabled=true exige qualitos.mail.from (adresse d'expédition)");
        }
        this.sender = sender;
        this.from = props.getFrom();
    }

    @Override
    public void send(String to, String subject, String body) {
        MimeMessage message = sender.createMimeMessage();
        try {
            // Multipart : le logo voyage DANS le message, et la version texte
            // accompagne la version HTML.
            MimeMessageHelper aide = new MimeMessageHelper(message, true, "UTF-8");
            aide.setFrom(from);
            aide.setTo(to);
            aide.setSubject(singleLine(subject));
            aide.setText(body == null ? "" : body, MailTemplate.plateforme(body));
            // Apres setText : le cadre range les elements en ligne dans la partie
            // qu'il vient de creer.
            aide.addInline(MailTemplate.LOGO_CID,
                    new ByteArrayResource(MailTemplate.logo()), "image/png");
        } catch (MessagingException e) {
            // Un rappel qui ne se prepare pas est une anomalie du programme, pas
            // une reponse du relais : on ne l'avale pas silencieusement.
            throw new IllegalStateException("Preparation du rappel impossible.", e);
        }
        sender.send(message);
    }

    /**
     * Écrase retours chariot et sauts de ligne du sujet.
     *
     * <p>Le sujet porte le titre de l'audit, saisi par un utilisateur. Un en-tête
     * de courriel se termine par CRLF : y glisser un saut de ligne permettrait
     * d'en forger d'autres à la suite — un {@code Bcc:} vers un tiers, par
     * exemple — et le rappel partirait où l'auteur du titre l'aurait décidé.
     *
     * <p>Aujourd'hui l'attaque échoue déjà, mais par accident : le préfixe du
     * sujet contient des accents, ce qui force JavaMail à encoder le sujet
     * ENTIER en RFC 2047, CRLF compris. Cette protection tient donc à la
     * typographie d'une chaîne de caractères — que la moindre traduction ou
     * refonte du libellé ferait disparaître, sans que rien ne le signale. On ne
     * laisse pas une propriété de sécurité dépendre d'un détail de rédaction.
     *
     * <p>Le corps n'a pas besoin du même traitement : il est envoyé comme
     * contenu, pas comme en-tête, et un saut de ligne y est légitime.
     */
    static String singleLine(String subject) {
        return subject == null ? null : subject.replaceAll("[\\r\\n]+", " ");
    }
}
