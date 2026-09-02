package com.openlab.qualitos.core.common;

/**
 * L'attribution d'une action (qui a fixé ce tarif, qui a créé cet
 * abonnement…) exige l'identité de l'utilisateur authentifié, mais le
 * principal de la requête n'en fournit aucune exploitable : absent, non
 * authentifié, ou un {@code sub} qui n'est pas un UUID — jeton de compte de
 * service, principal non-JWT, claim personnalisé.
 *
 * <p>Distincte d'une {@code IllegalArgumentException} laissée remonter telle
 * quelle : celle-ci tombait dans le catch-all du {@code GlobalExceptionHandler}
 * et rendait un 500 générique sur une action d'administration, alors que le
 * problème n'est ni une panne serveur ni un refus de rôle — c'est l'identité
 * elle-même qui n'est pas exploitable. Voir {@link CurrentUser#requireUserId()}
 * et le handler dédié dans {@code GlobalExceptionHandler} (401).
 */
public class UnresolvableActorException extends RuntimeException {

    public UnresolvableActorException() {
        super("Impossible de resoudre l'identite de l'utilisateur authentifie "
                + "(sub absent, non authentifie, ou non UUID)");
    }
}
