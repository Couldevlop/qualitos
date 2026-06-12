package com.openlab.qualitos.quality.visiongateway;

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
 * appels serveur-à-serveur engine → ai-vision-5s (ADR 0021). Port/adapter minimal :
 * la passerelle {@link VisionGatewayClient} ne connaît que {@link #getToken()}.
 *
 * <p>Comportement :
 * <ul>
 *   <li>POST {@code grant_type=client_credentials} vers {@code qualitos.vision.token-uri}
 *       (token endpoint Keycloak), client/secret injectés par env/Vault — jamais en clair
 *       dans le code ni dans les logs (§18.2-3, §22-9) ;</li>
 *   <li>cache du jeton jusqu'à {@code expires_in - marge} (marge configurable, 30 s par
 *       défaut), rafraîchissement thread-safe en double-vérification ;</li>
 *   <li>timeouts courts dédiés (le token endpoint doit répondre vite) ;</li>
 *   <li>échec (config incomplète, réseau, réponse invalide) → {@link VisionUnavailableException}
 *       : fail-closed, propagé en 503 « vision-unavailable » par le GlobalExceptionHandler,
 *       AUCUN repli silencieux vers le mode dev-claims.</li>
 * </ul>
 */
@Component
public class ServiceTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(ServiceTokenProvider.class);

    /** Jeton mis en cache avec sa date limite d'utilisation (expiration moins la marge). */
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

    public ServiceTokenProvider(
            @Value("${qualitos.vision.token-uri:}") String tokenUri,
            @Value("${qualitos.vision.client-id:api-quality-engine-vision}") String clientId,
            @Value("${qualitos.vision.client-secret:}") String clientSecret,
            @Value("${qualitos.vision.scope:}") String scope,
            @Value("${qualitos.vision.token-refresh-margin-s:30}") long refreshMarginSeconds,
            @Value("${qualitos.vision.token-connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${qualitos.vision.token-read-timeout-ms:5000}") int readTimeoutMs) {
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
     * Renvoie un jeton d'accès valide (réutilise le cache tant que l'expiration moins la
     * marge n'est pas atteinte, sinon rafraîchit). Thread-safe.
     *
     * @throws VisionUnavailableException config incomplète, token endpoint injoignable
     *                                    ou réponse invalide (fail-closed → 503)
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
            // Fail-closed : en mode bearer, une config incomplète rend le service vision
            // indisponible (503), on ne retombe JAMAIS silencieusement sur dev-claims.
            throw new VisionUnavailableException(
                    "Configuration du jeton de service vision incomplète : "
                            + "qualitos.vision.token-uri, client-id et client-secret sont requis en mode bearer");
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
                throw new VisionUnavailableException(
                        "Réponse du token endpoint sans access_token exploitable");
            }
            long expiresIn = resp.get("expires_in") instanceof Number n ? n.longValue() : 0L;
            Instant usableUntil = Instant.now().plusSeconds(Math.max(0, expiresIn - refreshMarginSeconds));
            // Journal structuré SANS la valeur du jeton ni le secret (§22-9).
            log.info("vision.token.refreshed client_id={} expires_in_s={}", clientId, expiresIn);
            return new CachedToken(token, usableUntil);
        } catch (RestClientException e) {
            // Le message d'erreur HTTP ne contient ni le secret ni un jeton.
            throw new VisionUnavailableException(
                    "Obtention du jeton de service vision impossible : " + e.getMessage(), e);
        }
    }
}
