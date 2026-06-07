package com.openlab.qualitos.quality.commconnector;

/**
 * Fournisseurs de communication supportés en V1 (CLAUDE.md §13.3 « Communication »).
 *
 * <p>Tous reposent sur un <b>incoming webhook</b> sortant (une URL fournie par
 * l'outil de chat). Aucune authentification supplémentaire n'est requise : l'URL
 * EST le secret (elle porte un jeton non devinable), elle est donc chiffrée au repos
 * via {@code quality/itsm/SecretCipher} comme les autres credentials.
 *
 * <p>Extension future : DISCORD, GOOGLE_CHAT, WEBEX… — ajouter une valeur ici et une
 * implémentation {@link CommProviderClient}.
 */
public enum CommProvider {
    TEAMS,
    SLACK,
    MATTERMOST
}
