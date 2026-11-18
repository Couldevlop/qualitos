package com.openlab.qualitos.core.mail;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Construit l'envoyeur d'une organisation à partir de son réglage.
 *
 * <p>Les <b>délais d'attente</b> ne sont pas un détail d'exploitation, ils sont
 * la raison d'être de cette classe. Par défaut, une connexion SMTP qui n'aboutit
 * pas attend <i>indéfiniment</i> : un serveur mal saisi bloquerait alors le fil
 * d'exécution qui sert la requête, puis le suivant, jusqu'à épuiser le pool et
 * emporter l'API entière — pour une faute de frappe dans un formulaire de
 * réglage. Dix secondes, et l'on rend la main avec un motif.
 */
@Component
public class MailSenderFactory {

    private final MailHostPolicy policy;

    public MailSenderFactory(MailHostPolicy policy) {
        this.policy = policy;
    }

    /** Assez pour un serveur lent, trop peu pour immobiliser un fil d'exécution. */
    private static final String DELAI_MS = "10000";

    public JavaMailSender creer(MailAccount reglage, String motDePasseClair) {
        // LA GARDE EST ICI, et pas seulement dans l'envoi d'essai. C'est le
        // seul endroit par lequel passent TOUS les chemins qui atteignent le
        // reseau -- l'essai comme l'envoi reel. La poser dans la sonde
        // uniquement laissait un contournement evident : enregistrer un hote
        // interne, activer la messagerie, et ne jamais appuyer sur « essai ».
        // Le premier courriel emis faisait alors la connexion qu'on voulait
        // interdire.
        String refus = policy.refus(reglage.getHost());
        if (refus != null) {
            throw new MailHostRefusedException(refus);
        }
        JavaMailSenderImpl envoyeur = new JavaMailSenderImpl();
        envoyeur.setHost(reglage.getHost());
        envoyeur.setPort(reglage.getPort());
        envoyeur.setDefaultEncoding("UTF-8");

        if (reglage.getUsername() != null && !reglage.getUsername().isBlank()) {
            envoyeur.setUsername(reglage.getUsername());
            envoyeur.setPassword(motDePasseClair);
        }

        Properties p = envoyeur.getJavaMailProperties();
        p.put("mail.transport.protocol", "smtp");
        p.put("mail.smtp.connectiontimeout", DELAI_MS);
        p.put("mail.smtp.timeout", DELAI_MS);
        p.put("mail.smtp.writetimeout", DELAI_MS);
        // On n'authentifie que si l'on a un compte : l'exiger interdirait le
        // relais interne qui reconnait ses clients a leur adresse IP.
        p.put("mail.smtp.auth",
                String.valueOf(reglage.getUsername() != null && !reglage.getUsername().isBlank()));

        switch (reglage.getSecurity()) {
            case TLS -> {
                // TLS des l'ouverture (465).
                p.put("mail.smtp.ssl.enable", "true");
                p.put("mail.smtp.ssl.checkserveridentity", "true");
            }
            case STARTTLS -> {
                p.put("mail.smtp.starttls.enable", "true");
                // REQUIRED, et non « enable » seul : sans cela, un serveur qui
                // n'annonce pas STARTTLS fait retomber la connexion en clair
                // SANS rien dire, et le mot de passe du client part en clair
                // sur le reseau. Mieux vaut un echec franc.
                p.put("mail.smtp.starttls.required", "true");
                p.put("mail.smtp.ssl.checkserveridentity", "true");
            }
            case NONE -> {
                p.put("mail.smtp.ssl.enable", "false");
                p.put("mail.smtp.starttls.enable", "false");
            }
        }
        return envoyeur;
    }
}
