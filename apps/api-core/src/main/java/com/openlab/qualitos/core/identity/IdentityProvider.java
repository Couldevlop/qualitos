package com.openlab.qualitos.core.identity;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Les comptes de connexion, quel que soit le fournisseur d'identité (CLAUDE.md §21 :
 * « abstraction IdentityProvider », pour ne pas s'enfermer dans Keycloak).
 *
 * <p>Le fournisseur porte ce que le jeton dira de l'utilisateur : son client
 * ({@code tenant_id}) et ses rôles de plateforme. QualitOS ne garde que le lien
 * (identifiant de compte) et ce qu'il administre lui-même (ADR 0078).
 *
 * <p>Toutes les méthodes échouent franchement ({@link IdentityProviderException})
 * plutôt que de rendre la main comme si le compte existait.
 */
public interface IdentityProvider {

    /** Crée le compte, le rattache au client et lui donne ses rôles ; dit comment il entrera. */
    CreatedAccount create(NewAccount account);

    /** Remplace les rôles de plateforme du compte ; ceux que QualitOS n'administre pas sont laissés. */
    void setRoles(String accountId, Set<String> roles);

    void setEnabled(String accountId, boolean enabled);

    /**
     * Ferme TOUTES les sessions ouvertes de ce compte, sur-le-champ.
     *
     * <p>Desactiver un compte ne suffit pas a en retirer l'acces. Le fournisseur
     * d'identite refuse alors de delivrer de NOUVEAUX jetons, mais celui qu'une
     * personne a deja dans son navigateur reste valable jusqu'a son echeance :
     * elle continue de lire, d'ecrire et de signer pendant tout ce temps, avec
     * un compte qu'un administrateur croit ferme.
     *
     * <p>C'est precisement l'ecart entre DESACTIVER et REVOQUER. Une revocation
     * qui laisse courir la session en cours n'est pas une revocation -- et on
     * la declenche justement les jours ou l'on ne peut pas attendre : un depart
     * conflictuel, un compte soupconne d'etre compromis.
     *
     * <p>L'operation est IDEMPOTENTE : fermer les sessions d'un compte qui n'en
     * a aucune reussit sans rien faire.
     */
    void logout(String accountId);

    /** Le client auquel le compte est rattaché, s'il existe. */
    Optional<UUID> tenantOf(String accountId);

    /** Compensation : supprime un compte qu'on vient de créer quand la suite a échoué. */
    void delete(String accountId);

    /** Le compte à créer. Le client vient toujours du serveur (jeton ou création du client), jamais du corps. */
    record NewAccount(UUID tenantId, String email, String firstName, String lastName, Set<String> roles) {}

    /**
     * Le compte créé.
     *
     * @param temporaryPassword le mot de passe provisoire, à montrer UNE fois — ou
     *                          {@code null} quand l'invitation part par e-mail
     */
    record CreatedAccount(String accountId, String temporaryPassword, boolean invitationSent) {}
}
