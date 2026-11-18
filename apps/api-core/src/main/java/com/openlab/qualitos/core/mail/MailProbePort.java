package com.openlab.qualitos.core.mail;

/**
 * Port sortant : l'envoi d'essai qui prouve qu'un réglage marche.
 *
 * <p>Il existe pour que la règle « un réglage non prouvé n'est pas vérifié » se
 * teste sans serveur SMTP, et pour qu'aucune suite de tests n'ouvre de
 * connexion sortante.
 *
 * <p>Le mot de passe passe ici <b>en clair</b> : c'est le seul endroit où il
 * l'est, le temps d'un appel, parce que c'est la forme sous laquelle le serveur
 * de messagerie l'attend. Il n'est ni journalisé, ni conservé, ni renvoyé.
 */
public interface MailProbePort {

    /**
     * Tente une connexion et un envoi réel vers {@code destinataire}.
     *
     * @return le motif de l'échec, ou {@code null} si l'essai a abouti. On rend
     *         le motif plutôt que de lever : un nom de serveur faux n'est pas
     *         une anomalie du programme, c'est une réponse attendue de cet
     *         appel, et la personne qui règle sa messagerie doit la lire.
     */
    String essayer(MailAccount reglage, String motDePasseClair, String destinataire);
}
