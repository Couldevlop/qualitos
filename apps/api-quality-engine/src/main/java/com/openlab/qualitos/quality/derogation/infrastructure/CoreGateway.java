package com.openlab.qualitos.quality.derogation.infrastructure;

import com.openlab.qualitos.quality.derogation.application.DerogationMailException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * L'accès du moteur à {@code api-core}, pour l'annuaire et pour l'envoi au client.
 *
 * <p><b>Le jeton de l'appelant est PROPAGÉ, aucun jeton de service n'est forgé.</b>
 * C'est le choix structurant de cette classe, repris de
 * {@code QualityEngineActivationAdapter} qui l'a fait dans l'autre sens. Trois
 * raisons :
 *
 * <ul>
 *   <li><b>Le client vient du jeton, pas du corps.</b> {@code api-core} lit le
 *       {@code tenant_id} du jeton qu'on lui transmet, exactement comme s'il avait
 *       reçu la requête directement. Un jeton de service obligerait à lui dire
 *       <i>de quel client</i> il s'agit dans le corps de l'appel — ce que la
 *       règle 18.2 #2 interdit, et pour un relais de messagerie sous l'identité
 *       d'un client, ce serait une usurpation à une requête près.</li>
 *   <li><b>L'acte reste attribuable.</b> Le courriel part au nom de la personne
 *       qui l'a décidé, et les deux journaux — le moteur ici, {@code api-core}
 *       là-bas — nomment le même acteur.</li>
 *   <li><b>Aucun secret de plus.</b> Pas de client Keycloak supplémentaire, pas de
 *       secret à faire tourner (§18.2 #3).</li>
 * </ul>
 *
 * <p>{@code api-core} refait sa propre vérification : la propagation ne lui
 * demande pas de faire confiance au moteur, seulement de lire un jeton qu'il
 * aurait pu recevoir lui-même.
 *
 * <p><b>Échec fermé sur la configuration.</b> Sans adresse de base, on ne peut
 * ni résoudre un manager ni envoyer un courriel : on le dit, au lieu de rendre la
 * main comme si tout allait bien.
 */
@Component
public class CoreGateway {

    private final String baseUrl;
    private final RestClient client;

    public CoreGateway(@Value("${qualitos.core.base-url:}") String baseUrl,
                       RestClient.Builder builder) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        this.client = builder.build();
    }

    boolean estConfigure() {
        return !baseUrl.isEmpty();
    }

    String base() {
        if (baseUrl.isEmpty()) {
            throw new DerogationMailException(
                    "qualitos.core.base-url n'est pas configuré : le moteur ne peut"
                            + " joindre api-core ni pour l'annuaire ni pour l'envoi.");
        }
        return baseUrl;
    }

    RestClient rest() {
        return client;
    }

    /**
     * Le jeton brut de la requête en cours.
     *
     * <p>Hors contexte d'une requête authentifiée — un traitement planifié, un
     * test — il n'y en a pas, et l'appelant doit le traiter comme un refus et non
     * comme une permission.
     */
    Optional<String> jetonPropage() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return Optional.of(jwt.getToken().getTokenValue());
        }
        return Optional.empty();
    }

    /** L'en-tête d'autorisation à poser sur un appel sortant. */
    void autoriser(HttpHeaders entetes) {
        entetes.setBearerAuth(jetonPropage().orElseThrow(() -> new DerogationMailException(
                "Aucun jeton à propager : cet appel doit partir d'une requête authentifiée.")));
    }
}
