package com.openlab.qualitos.quality.erpconnector;

import java.util.List;

/**
 * Abstraction d'un client provider ERP. Une impl par {@link ErpProvider}.
 *
 * <p>Le secret en clair (déchiffré juste avant l'appel) est passé en paramètre — la
 * connexion entité (qui contient le ciphertext) sert juste à fournir baseUrl / username
 * / scope. Le service ne logue JAMAIS le secret.
 */
public interface ErpProviderClient {

    /** Provider couvert par cette implémentation. */
    ErpProvider provider();

    /**
     * Récupère les fournisseurs (achats) côté ERP.
     *
     * @param connection          configuration (baseUrl, username, scope) — secret EXCLU
     * @param plaintextCredential secret en clair (déchiffré juste avant l'appel)
     * @return liste de fournisseurs normalisés
     * @throws ErpSyncException sur erreur HTTP / auth / parsing
     */
    List<ExternalSupplier> fetchSuppliers(ErpConnection connection, String plaintextCredential);

    /**
     * Récupère les indicateurs de production côté ERP.
     *
     * @param connection          configuration (baseUrl, username, scope) — secret EXCLU
     * @param plaintextCredential secret en clair (déchiffré juste avant l'appel)
     * @return liste d'indicateurs normalisés
     * @throws ErpSyncException sur erreur HTTP / auth / parsing
     */
    List<ExternalProductionKpi> fetchProductionKpis(ErpConnection connection, String plaintextCredential);
}
