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
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }
}
