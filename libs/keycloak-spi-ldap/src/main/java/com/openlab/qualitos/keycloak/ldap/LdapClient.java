package com.openlab.qualitos.keycloak.ldap;

import java.util.Optional;

/**
 * Abstraction testable des opérations LDAP nécessaires au provider. Permet de
 * mocker le bind et la recherche sans serveur réel (cf. tests unitaires), tandis
 * que l'implémentation de production {@link JndiLdapClient} s'appuie sur
 * {@code javax.naming.ldap} (JNDI), sans dépendance lourde.
 *
 * <p>Contrat des exceptions :
 * <ul>
 *   <li>une <strong>indisponibilité</strong> de l'annuaire (timeout, connexion
 *       refusée, bind de service en échec d'infra) lève une
 *       {@link LdapUnavailableException} → le provider applique le fallback ;</li>
 *   <li>un utilisateur <strong>absent</strong> renvoie {@link Optional#empty()} ;</li>
 *   <li>un mot de passe <strong>invalide</strong> renvoie {@code false} depuis
 *       {@link #authenticate(String, String)} (sans exception).</li>
 * </ul>
 */
public interface LdapClient {

    /**
     * Recherche une entrée par la valeur d'un attribut (username ou email), en
     * appliquant le filtre configuré.
     *
     * @param attribute nom de l'attribut LDAP à matcher (déjà résolu via la config)
     * @param value     valeur recherchée (sera échappée par l'implémentation)
     * @return l'entrée mappée, ou vide si aucune correspondance
     * @throws LdapUnavailableException si l'annuaire est injoignable
     */
    Optional<LdapUserEntry> findByAttribute(String attribute, String value);

    /**
     * Recherche une entrée par son identifiant stable (uuidAttribute).
     *
     * @throws LdapUnavailableException si l'annuaire est injoignable
     */
    Optional<LdapUserEntry> findByUuid(String uuid);

    /**
     * Liste paginée d'entrées (pour {@code searchForUser} de Keycloak).
     *
     * @param firstResult offset (≥ 0)
     * @param maxResults  taille de page (≤ 0 ⇒ illimité)
     * @throws LdapUnavailableException si l'annuaire est injoignable
     */
    java.util.List<LdapUserEntry> searchAll(int firstResult, int maxResults);

    /**
     * Valide un mot de passe en effectuant un bind LDAP sur le DN de l'utilisateur.
     *
     * @param userDn   DN complet de l'utilisateur (issu d'une entrée déjà résolue)
     * @param password mot de passe en clair fourni par l'utilisateur
     * @return {@code true} si le bind réussit ; {@code false} si les identifiants
     *         sont invalides
     * @throws LdapUnavailableException si l'annuaire est injoignable (≠ mauvais mot de passe)
     */
    boolean authenticate(String userDn, String password);
}
