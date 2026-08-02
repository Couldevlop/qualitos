package com.openlab.qualitos.quality.aigateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.Map;

/**
 * Fournisseur de jeton de service OAuth2 <b>client_credentials</b> (Keycloak) pour les
 * appels serveur-à-serveur engine → ai-service (ADR 0014).
 *
 * <p><b>Pourquoi cette classe existe.</b> La documentation d'{@link AiGatewayClient}
 * annonçait depuis l'origine « en prod : jeton OIDC client_credentials d'audience
 * qualitos-ai », mais ce chemin n'avait jamais été implémenté : la passerelle envoyait
 * toujours l'en-tête {@code X-Dev-Claims}. Tant que l'ai-service tournait avec
 * {@code QOS_DEV_AUTH=1} le défaut restait invisible ; au premier déploiement en
 * configuration de production, l'ai-service a répondu 401 et toutes les fonctions d'IA
 * sont tombées. Le mécanisme est ici transposé de {@code visiongateway.ServiceTokenProvider}
 * (ADR 0021), qui l'avait déjà résolu pour le service de vision.
 *
 * <p>Comportement :
 * <ul>
 *   <li>POST {@code grant_type=client_credentials} vers {@code qualitos.ai.token-uri},
 *       client et secret injectés par l'environnement — jamais en clair dans le code ni
 *       dans les journaux (§18.2-3, §22-9) ;</li>
 *   <li>cache du jeton jusqu'à {@code expires_in} moins une marge, rafraîchissement
 *       thread-safe en double vérification ;</li>
 *   <li>timeouts courts dédiés : un point d'émission de jeton doit répondre vite, et
 *       l'attente ne doit pas s'ajouter au délai déjà long des appels de modèle ;</li>
 *   <li>en cas d'échec — configuration incomplète, réseau, réponse inexploitable —
 *       {@link AiGatewayException}. <b>Fail-closed</b> : on ne retombe JAMAIS en silence
 *       sur {@code X-Dev-Claims}, qui laisserait n'importe quel appelant se déclarer
 *       porteur de n'importe quel tenant.</li>
 * </ul>
 */
@Component
public class AiServiceTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(AiServiceTokenProvider.class);

    /** Jeton en cache avec sa date limite d'utilisation (expiration moins la marge). */
    private record CachedToken(String value, Instant usableUntil) {
    }

    private final String tokenUri;
    private final String clientId;
    private final String clientSecret;
    private final String scope;
    private final long refreshMarginSeconds;
    private final RestClient client;

    private final Object refreshLock = new Object();
    private volatile CachedToken cached;

    public AiServiceTokenProvider(
            @Value("${qualitos.ai.token-uri:}") String tokenUri,
            @Value("${qualitos.ai.client-id:api-quality-engine-ai}") String clientId,
            @Value("${qualitos.ai.client-secret:}") String clientSecret,
            @Value("${qualitos.ai.scope:}") String scope,
            @Value("${qualitos.ai.token-refresh-margin-s:30}") long refreshMarginSeconds,
            @Value("${qualitos.ai.token-connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${qualitos.ai.token-read-timeout-ms:5000}") int readTimeoutMs) {
        this.tokenUri = tokenUri == null ? "" : tokenUri.trim();
        this.clientId = clientId == null ? "" : clientId.trim();
        this.clientSecret = clientSecret == null ? "" : clientSecret;
        this.scope = scope == null ? "" : scope.trim();
        this.refreshMarginSeconds = Math.max(0, refreshMarginSeconds);
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(connectTimeoutMs);
        rf.setReadTimeout(readTimeoutMs);
        this.client = RestClient.builder().requestFactory(rf).build();
    }

    /**
     * Renvoie un jeton d'accès valide : réutilise le cache tant que l'expiration moins la
     * marge n'est pas atteinte, rafraîchit sinon. Thread-safe.
     *
     * @throws AiGatewayException configuration incomplète, point d'émission injoignable
     *                            ou réponse inexploitable
     */
    public String getToken() {
        CachedToken current = cached;
        if (current != null && Instant.now().isBefore(current.usableUntil())) {
            return current.value();
        }
        synchronized (refreshLock) {
            current = cached;
            if (current != null && Instant.now().isBefore(current.usableUntil())) {
                return current.value();
            }
            CachedToken fresh = fetchToken();
            cached = fresh;
            return fresh.value();
        }
    }

    @SuppressWarnings("unchecked")
    private CachedToken fetchToken() {
        if (tokenUri.isBlank() || clientId.isBlank() || clientSecret.isBlank()) {
            throw new AiGatewayException(
                    "Configuration du jeton de service IA incomplète : qualitos.ai.token-uri, "
                            + "client-id et client-secret sont requis en mode bearer");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        if (!scope.isBlank()) {
            form.add("scope", scope);
        }
        try {
            Map<String, Object> resp = client.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);
            if (resp == null || !(resp.get("access_token") instanceof String token) || token.isBlank()) {
                throw new AiGatewayException("Réponse du point d'émission sans access_token exploitable");
            }
            long expiresIn = resp.get("expires_in") instanceof Number n ? n.longValue() : 0L;
            Instant usableUntil = Instant.now().plusSeconds(Math.max(0, expiresIn - refreshMarginSeconds));
            // Journal structuré SANS la valeur du jeton ni le secret (§22-9).
            log.info("ai.token.refreshed client_id={} expires_in_s={}", clientId, expiresIn);
            return new CachedToken(token, usableUntil);
        } catch (RestClientException e) {
            // Le message d'erreur HTTP ne contient ni le secret ni un jeton.
            throw new AiGatewayException(
                    "Obtention du jeton de service IA impossible : " + e.getMessage(), e);
        }
    }
}
