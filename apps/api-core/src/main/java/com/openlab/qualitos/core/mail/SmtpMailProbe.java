package com.openlab.qualitos.core.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * L'envoi d'essai : il se connecte pour de vrai, et dit ce qui a cloché.
 *
 * <p>Un essai qui se contenterait d'ouvrir la connexion ne prouverait pas
 * grand-chose. Le serveur peut accepter la connexion, accepter le compte, puis
 * <b>refuser l'expéditeur</b> — c'est même le cas le plus fréquent, quand
 * l'adresse d'expédition n'est pas autorisée sur le compte. On envoie donc un
 * message complet : ce qu'on vérifie, c'est ce qui se passera en production.
 */
@Component
public class SmtpMailProbe implements MailProbePort {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailProbe.class);

    private static final String SUJET = "QualitOS - essai de configuration";
    private static final String CORPS = """
            Ce message confirme que la messagerie de votre organisation est
            correctement configuree dans QualitOS.

            Les courriels emis par la plateforme partiront desormais de cette
            adresse.""";

    private final MailSenderFactory factory;
    private final MailHostPolicy policy;

    public SmtpMailProbe(MailSenderFactory factory, MailHostPolicy policy) {
        this.factory = factory;
        this.policy = policy;
    }

    @Override
    public String essayer(MailAccount reglage, String motDePasseClair, String destinataire) {
        String refus = policy.refus(reglage.getHost());
        if (refus != null) {
            return refus;
        }
        try {
            JavaMailSender envoyeur = factory.creer(reglage, motDePasseClair);
            MimeMessage message = envoyeur.createMimeMessage();
            MimeMessageHelper aide = new MimeMessageHelper(message, false, "UTF-8");
            aide.setFrom(expediteur(reglage));
            aide.setTo(destinataire);
            if (reglage.getReplyTo() != null && !reglage.getReplyTo().isBlank()) {
                aide.setReplyTo(reglage.getReplyTo());
            }
            aide.setSubject(SUJET);
            aide.setText(CORPS, false);
            envoyeur.send(message);
            return null;
        } catch (MessagingException | MailException e) {
            // On NE LEVE PAS : un nom de serveur faux ou un mot de passe refuse
            // n'est pas une anomalie du programme, c'est la reponse attendue de
            // cet appel. La personne qui regle sa messagerie doit la lire.
            log.info("mail.probe.failed host={} port={} raison={}",
                    reglage.getHost(), reglage.getPort(), e.getClass().getSimpleName());
            return motifLisible(e);
        }
    }

    private static String expediteur(MailAccount reglage) throws MessagingException {
        if (reglage.getFromName() == null || reglage.getFromName().isBlank()) {
            return reglage.getFromAddress();
        }
        try {
            return new jakarta.mail.internet.InternetAddress(
                    reglage.getFromAddress(), reglage.getFromName(), "UTF-8").toString();
        } catch (java.io.UnsupportedEncodingException e) {
            // UTF-8 est garanti par la plateforme Java ; si elle manquait, le
            // nom n'est pas une raison d'echouer -- l'adresse suffit.
            return reglage.getFromAddress();
        }
    }

    /**
     * Le motif, tel qu'un administrateur peut l'exploiter.
     *
     * <p>On garde le message du serveur, qui porte le code SMTP (535 pour un
     * compte refusé, 550 pour un expéditeur interdit) : c'est ce qui permet de
     * corriger. On y ajoute la cause racine quand elle existe, parce que
     * « Mail server connection failed » seul ne distingue pas un port fermé
     * d'un nom introuvable.
     */
    private static String motifLisible(Exception e) {
        StringBuilder motif = new StringBuilder(
                e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        Throwable cause = e.getCause();
        if (cause != null && cause.getMessage() != null
                && !motif.toString().contains(cause.getMessage())) {
            motif.append(" — ").append(cause.getMessage());
        }
        // Un message de serveur peut etre long ; l'ecran n'en montrera qu'une
        // ligne, et un pave illisible decourage autant qu'une absence de motif.
        String texte = motif.toString().replaceAll("\\s+", " ").strip();
        return texte.length() > 400 ? texte.substring(0, 400) + "..." : texte;
    }
}
