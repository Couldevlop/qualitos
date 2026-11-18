package com.openlab.qualitos.core.mail;

/**
 * Comment la connexion au serveur sortant est protégée.
 *
 * <p>Trois valeurs, et pas un drapeau booléen : « TLS » et « STARTTLS » ne
 * sont pas deux façons de dire la même chose. Le premier chiffre dès la
 * connexion (port 465), le second ouvre en clair puis négocie (port 587). Les
 * confondre donne une connexion qui expire sans message utile, et c'est la
 * première cause d'échec d'un réglage SMTP.
 */
public enum MailSecurity {

    /** Aucun chiffrement. Légitime pour un relais interne, jamais vers Internet. */
    NONE,

    /** Connexion en clair, puis passage en TLS à la commande STARTTLS (587). */
    STARTTLS,

    /** TLS dès l'ouverture de la connexion (465). */
    TLS
}
