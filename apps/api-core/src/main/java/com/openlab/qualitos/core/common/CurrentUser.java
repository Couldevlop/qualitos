package com.openlab.qualitos.core.common;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

/**
 * Résout l'utilisateur courant (le {@code sub} du JWT) depuis le
 * {@link SecurityContextHolder}.
 *
 * <p><b>Jumelle volontaire.</b> Le moteur de qualité porte un
 * {@code CurrentUser} qui fait exactement ce garde-fou
 * ({@code com.openlab.qualitos.quality.common.CurrentUser}) : même motif que
 * {@link com.openlab.qualitos.core.billing.BillingTier} — {@code api-core} et
 * {@code api-quality-engine} sont deux modules Maven FRÈRES, aucun ne peut
 * importer l'autre, et une bibliothèque partagée pour une classe de cette
 * taille coûterait plus cher que la copie assumée. Cette copie se limite à la
 * résolution d'acteur ; {@code api-core} n'a pas (encore) besoin d'un
 * {@code displayName()}.
 *
 * <p>Invariant de sécurité (OWASP A01) : l'attribution d'acteur d'une action
 * (qui a fixé ce tarif, qui a créé cet abonnement…) provient TOUJOURS de
 * l'identité authentifiée, jamais du corps de la requête, qui est
 * falsifiable.
 *
 * <p><b>Pourquoi une classe plutôt qu'un {@code try/catch} sur place.</b> Le
 * même geste ({@code UUID.fromString(authentication.getName())} protégé)
 * revient à chaque colonne d'audit — {@code updated_by} ici, bientôt
 * {@code created_by} et {@code cancelled_by} des abonnements, l'émetteur des
 * factures. Répéter le {@code try/catch} quatre fois est exactement la façon
 * dont l'un des quatre finit par être oublié ; centraliser élimine le motif
 * plutôt que de le refaire correctement à chaque fois — même raisonnement que
 * le bean {@code Clock} unique de {@code ClockConfig}.
 */
public final class CurrentUser {

    private CurrentUser() {}

    /**
     * @return le {@code sub} du JWT typé en UUID si présent et bien formé,
     * sinon {@link Optional#empty()} — jamais d'exception, pour l'appelant
     * qui préfère décider lui-même plutôt que se faire imposer un refus.
     */
    public static Optional<UUID> userId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !StringUtils.hasText(auth.getName())) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(auth.getName()));
        } catch (IllegalArgumentException ex) {
            // sub present mais pas un UUID exploitable (jeton de compte de
            // service, principal non-JWT, claim personnalise) : un echec de
            // parsing n'est pas une panne, c'est une identite non exploitable.
            return Optional.empty();
        }
    }

    /**
     * @return le {@code sub} du JWT typé en UUID, ou lève
     * {@link UnresolvableActorException} quand il est absent ou malformé.
     * Traduite en 401 par {@code GlobalExceptionHandler} — ni une panne
     * serveur (500), ni un refus de rôle (403) : l'identité elle-même n'est
     * pas exploitable.
     */
    public static UUID requireUserId() {
        return userId().orElseThrow(UnresolvableActorException::new);
    }
}
