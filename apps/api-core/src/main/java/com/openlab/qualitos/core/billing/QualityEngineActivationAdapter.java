package com.openlab.qualitos.core.billing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * Adaptateur du port {@link ModuleActivationPort} : appelle la surface
 * {@code /api/v1/platform/tenants/{tenantId}/modules} du moteur de qualité.
 *
 * <p><b>Le jeton de l'appelant est PROPAGÉ, aucun jeton de service n'est
 * forgé.</b> C'est le choix structurant de cet adaptateur, et il est délibéré :
 * un jeton de service (motif {@code ServiceTokenProvider} du moteur vers
 * ai-vision-5s, ADR 0021) ferait apparaître « api-core » comme auteur de
 * l'activation dans le journal chaîné. §18.2 règle 5 exige l'inverse — un acte
 * commercial doit rester attribuable à la PERSONNE qui l'a décidé, des années
 * plus tard, quand plus personne ne se souviendra de qui a signé. En
 * transportant le jeton du {@code SUPER_ADMIN} qui souscrit, le moteur journalise
 * son {@code sub} à lui, et les deux journaux — commercial ici, chaîné là-bas —
 * nomment le même acteur.
 *
 * <p>Bénéfice second, non négligeable : aucun client Keycloak supplémentaire,
 * aucun secret de plus à faire tourner (§18.2 règle 3). Le moteur, de son côté,
 * refait sa propre vérification de rôle : la propagation ne lui demande pas de
 * faire confiance à {@code api-core}, seulement de lire le jeton qu'il aurait
 * reçu directement.
 *
 * <p><b>Aucun repli silencieux.</b> Moteur injoignable, refus, configuration
 * absente, requête hors contexte HTTP : tout se termine en
 * {@link ModuleActivationFailedException}, et {@link SubscriptionService}
 * abandonne l'abonnement. Le seul comportement qu'on refuse ici est de rendre
 * la main comme si tout allait bien.
 */
@Component
public class QualityEngineActivationAdapter implements ModuleActivationPort {

    private static final Logger log = LoggerFactory.getLogger(QualityEngineActivationAdapter.class);

    private final String baseUrl;
    private final RestClient client;

    public QualityEngineActivationAdapter(
            @Value("${qualitos.quality-engine.base-url:}") String baseUrl,
            RestClient.Builder builder) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.client = builder.build();
    }

    @Override
    public void activate(UUID tenantId, String moduleCode) {
        call("POST", tenantId, moduleCode);
    }

    @Override
    public void deactivate(UUID tenantId, String moduleCode) {
        call("DELETE", tenantId, moduleCode);
    }

    private void call(String verb, UUID tenantId, String moduleCode) {
        // Fail-closed sur la configuration : sans URL, on ne peut pas ouvrir de
        // module, donc on ne facture pas. Un adaptateur qui se tairait ici
        // laisserait passer des souscriptions dont aucun module ne serait
        // jamais ouvert — le pire des deux mondes.
        if (baseUrl.isBlank()) {
            throw new ModuleActivationFailedException(
                    "qualitos.quality-engine.base-url n'est pas configure : "
                            + "impossible d'appliquer une decision commerciale au moteur de qualite");
        }
        String bearer = currentBearerToken();
        String uri = baseUrl + "/api/v1/platform/tenants/" + tenantId + "/modules/" + moduleCode;
        try {
            RestClient.RequestHeadersSpec<?> request = "POST".equals(verb)
                    ? client.post().uri(uri)
                    : client.delete().uri(uri);
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
                    .retrieve()
                    .toBodilessEntity();
            // Journal structuré : le tenant et le module, jamais le jeton (§22-9).
            log.info("billing.module.{} tenant_id={} module_code={}",
                    "POST".equals(verb) ? "activated" : "deactivated", tenantId, moduleCode);
        } catch (RestClientException e) {
            // Le message d'une RestClientException porte le statut et l'URI,
            // pas l'en-tête Authorization : rien à expurger ici.
            throw new ModuleActivationFailedException(
                    "Le moteur de qualite a refuse d'appliquer la decision sur le module "
                            + moduleCode + " pour le client " + tenantId + " : " + e.getMessage(), e);
        }
    }

    /**
     * Le jeton porteur de la requête en cours.
     *
     * <p>Lu du {@code SecurityContextHolder} et non d'un en-tête recopié à la
     * main : le contexte de sécurité porte le jeton DÉJÀ VALIDÉ par la chaîne
     * de filtres (signature, expiration, émetteur). Relire l'en-tête brut
     * ferait suivre au moteur un jeton qu'{@code api-core} n'aurait pas
     * vérifié.
     *
     * <p>Un principal non-JWT (test, compte technique, jeton opaque) n'a pas de
     * valeur à propager : plutôt qu'appeler le moteur sans autorisation — qui
     * répondrait 401 et produirait un message trompeur — on refuse ici, en
     * nommant la cause.
     */
    private String currentBearerToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getTokenValue();
        }
        throw new ModuleActivationFailedException(
                "Aucun jeton JWT dans le contexte de securite : l'activation pour un client "
                        + "designe exige de propager l'identite de l'editeur au moteur de qualite");
    }
}
