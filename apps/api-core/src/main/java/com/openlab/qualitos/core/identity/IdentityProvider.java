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
