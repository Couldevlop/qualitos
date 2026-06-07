package com.openlab.qualitos.quality.commconnector;

/**
 * Abstraction d'un client de communication sortant. Une impl par {@link CommProvider}.
 *
 * <p>L'URL d'incoming-webhook en clair (déchiffrée juste avant l'appel) est passée en
 * paramètre — l'entité {@link CommConnection} (qui contient le ciphertext) sert juste à
 * fournir le canal optionnel. Le client ne logue JAMAIS l'URL (elle porte le jeton).
 */
public interface CommProviderClient {

    /** Provider couvert par cette implémentation. */
    CommProvider provider();

    /**
     * Poste le message sur l'incoming webhook.
     *
     * @param connection  configuration (channel) — secret EXCLU
     * @param webhookUrl  URL d'incoming-webhook en clair (déchiffrée juste avant l'appel)
     * @param message     message neutre à formater selon le provider
     * @throws CommSendException sur erreur HTTP / réseau / sérialisation
     */
    void send(CommConnection connection, String webhookUrl, CommMessage message);
}
