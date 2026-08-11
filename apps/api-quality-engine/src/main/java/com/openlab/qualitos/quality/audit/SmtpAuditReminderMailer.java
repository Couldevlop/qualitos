package com.openlab.qualitos.quality.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Envoi SMTP du rappel d'échéance (§4.4), sur le modèle exact de l'adaptateur S3 :
 * activé uniquement quand {@code qualitos.mail.enabled=true}. Sans cela, aucun bean
 * n'est créé et le rappel se limite à la notification interne.
 *
 * <p>Message en TEXTE BRUT, à dessein. Le corps porte le titre de l'audit, saisi
 * par un utilisateur ; en HTML il faudrait l'échapper, et un oubli d'échappement
 * dans un courriel se voit rarement avant qu'il ne serve (OWASP A03). Le texte
 * brut retire le problème au lieu de le gérer.
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(singleLine(subject));
        message.setText(body);
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
