package com.openlab.qualitos.quality.itsm;

import java.time.Instant;
import java.util.List;

/**
 * Abstraction d'un client provider ITSM. Une impl par {@link ItsmProvider}.
 *
 * Le secret en clair (déchiffré juste avant l'appel) est passé en paramètre — la
 * connexion entité (qui contient le ciphertext) sert juste à fournir baseUrl / username
 * / scope. Le service ne logue JAMAIS le secret.
 */
public interface ItsmProviderClient {

    /** Provider couvert par cette implémentation. */
    ItsmProvider provider();

    /**
     * Récupère les incidents mis à jour depuis {@code since}.
     *
     * @param connection         configuration (baseUrl, username, scope) — secret EXCLU
     * @param plaintextCredential secret en clair (déchiffré juste avant l'appel)
     * @param since              fenêtre temporelle, null → tout depuis l'origine
     * @return liste d'incidents normalisés
     * @throws ItsmSyncException sur erreur HTTP / auth / parsing
     */
    List<ExternalIncident> fetchIncidents(ItsmConnection connection, String plaintextCredential, Instant since);
}
