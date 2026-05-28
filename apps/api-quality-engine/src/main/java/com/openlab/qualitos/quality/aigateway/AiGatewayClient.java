package com.openlab.qualitos.quality.aigateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client serveur-à-serveur vers la passerelle IA {@code ai-service} (CLAUDE.md §12.2,
 * §18.2 règle 4 : tout appel LLM passe par la passerelle, qui applique redaction PII +
 * bouclier anti-injection). Le SPA appelle api-quality-engine (JWT utilisateur validé) ;
 * api-quality-engine relaie vers ai-service.
 *
 * <p>Auth (cf. ADR 0014) : en dev, en-tête {@code X-Dev-Claims} (ai-service avec
 * {@code QOS_DEV_AUTH=1}) — le tenant provient du {@link TenantContext} (JWT, jamais du body).
 * En prod : jeton OIDC client_credentials d'audience {@code qualitos-ai}.
 */
@Component
public class AiGatewayClient {

    /** Sujet de service (déterministe) porté dans X-Dev-Claims pour l'appel interne. */
    private static final UUID SERVICE_SUBJECT = UUID.fromString("0000000a-0000-0000-0000-000000000a01");

    private final RestClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiGatewayClient(
            @Value("${qualitos.ai.base-url:http://localhost:8085}") String baseUrl,
            @Value("${qualitos.ai.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${qualitos.ai.read-timeout-ms:260000}") int readTimeoutMs) {
        // Timeouts CONFIGURABLES (cf. ADR 0014). Le read-timeout doit rester
        // > OLLAMA_TIMEOUT_S côté ai-service (lui-même > latence du modèle). Pour un
        // modèle plus lent/précis sur CPU (ex. qualitos-sql-lite), relever de concert
        // qualitos.ai.read-timeout-ms ET OLLAMA_TIMEOUT_S — sans toucher au code.
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(connectTimeoutMs);
        rf.setReadTimeout(readTimeoutMs);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(rf).build();
    }

    @SuppressWarnings("unchecked")
    public AiCompletionResult complete(String systemPrompt, String userPrompt, int maxTokens) {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        String devClaims = devClaims(UUID.fromString(TenantContext.getTenantId()));
        Map<String, Object> body = Map.of(
                "system_prompt", systemPrompt,
                "user_prompt", userPrompt,
                "max_tokens", maxTokens,
                "temperature", 0.2);
        try {
            Map<String, Object> resp = client.post()
                    .uri("/v1/ai/complete")
                    .header("X-Dev-Claims", devClaims)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp == null || resp.get("text") == null) {
                throw new AiGatewayException("Réponse vide de la passerelle IA");
            }
            return new AiCompletionResult(
                    String.valueOf(resp.get("text")),
                    String.valueOf(resp.getOrDefault("provider", "")),
                    intValue(resp.get("tokens_used")),
                    intValue(resp.get("latency_ms")));
        } catch (RestClientException e) {
            throw new AiGatewayException("Passerelle IA indisponible : " + e.getMessage(), e);
        }
    }

    /**
     * Relaie une requête NLQ (langage naturel → SQL validé → exécution read-only) vers
     * {@code ai-service} (§7.3). Renvoie la réponse JSON brute (mappée par la couche
     * application). Le tenant provient du {@link TenantContext} (JWT), jamais du body.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> askNlq(String question, int maxRows) {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        String devClaims = devClaims(UUID.fromString(TenantContext.getTenantId()));
        Map<String, Object> body = Map.of("question", question, "max_rows", maxRows);
        try {
            Map<String, Object> resp = client.post()
                    .uri("/v1/ai/nlq/ask")
                    .header("X-Dev-Claims", devClaims)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp == null) {
                throw new AiGatewayException("Réponse vide de la passerelle IA (NLQ)");
            }
            return resp;
        } catch (RestClientException e) {
            throw new AiGatewayException("Passerelle IA indisponible (NLQ) : " + e.getMessage(), e);
        }
    }

    private String devClaims(UUID tenantId) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "sub", SERVICE_SUBJECT.toString(),
                    "tid", tenantId.toString(),
                    "roles", List.of("quality_manager")));
        } catch (Exception e) {
            throw new AiGatewayException("Sérialisation des claims impossible", e);
        }
    }

    private int intValue(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }
}
