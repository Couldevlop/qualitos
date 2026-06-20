package com.openlab.qualitos.quality.aigateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.ai.guard.AiCallContext;
import com.openlab.qualitos.quality.ai.guard.AiGuard;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
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
    private final AiGuard guard;

    public AiGatewayClient(
            @Value("${qualitos.ai.base-url:http://localhost:8085}") String baseUrl,
            @Value("${qualitos.ai.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${qualitos.ai.read-timeout-ms:260000}") int readTimeoutMs,
            AiGuard guard) {
        this.guard = guard;
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
        AiCallContext ctx = new AiCallContext(TenantContext.getTenantId(), "complete",
                (systemPrompt == null ? 0 : systemPrompt.length())
                        + (userPrompt == null ? 0 : userPrompt.length()));
        guard.check(ctx);
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
            guard.recordSuccess(ctx.tenantId());
            return new AiCompletionResult(
                    String.valueOf(resp.get("text")),
                    String.valueOf(resp.getOrDefault("provider", "")),
                    intValue(resp.get("tokens_used")),
                    intValue(resp.get("latency_ms")));
        } catch (RestClientException e) {
            guard.recordFailure(ctx.tenantId());
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
        AiCallContext ctx = new AiCallContext(TenantContext.getTenantId(), "nlq",
                question == null ? 0 : question.length());
        guard.check(ctx);
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
            guard.recordSuccess(ctx.tenantId());
            return resp;
        } catch (RestClientException e) {
            guard.recordFailure(ctx.tenantId());
            throw new AiGatewayException("Passerelle IA indisponible (NLQ) : " + e.getMessage(), e);
        }
    }

    /**
     * Relaie une série de mesures vers la détection d'anomalies SPC de {@code ai-service}
     * (§3.4, §12.1 : limites de contrôle + 8 règles de Nelson, NumPy). Renvoie la réponse
     * JSON brute (mappée par la couche application). Le tenant provient du {@link TenantContext}
     * (JWT), jamais du body. {@code center}/{@code sigma} optionnels (baseline connue).
     *
     * <p>Bien que l'analyse SPC soit statistique (pas un LLM), elle passe par le même
     * garde-fou ({@link AiGuard}) que les appels IA : cloisonnement du débit/quota par
     * tenant sur la passerelle (OWASP LLM04, cohérence des chemins sortants).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> detectSpc(List<Double> values, Double center, Double sigma) {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        String devClaims = devClaims(UUID.fromString(TenantContext.getTenantId()));
        // Map.of refuse les valeurs null → construire la map en tenant compte de la baseline.
        Map<String, Object> body = new HashMap<>();
        body.put("values", values);
        if (center != null) {
            body.put("center", center);
        }
        if (sigma != null) {
            body.put("sigma", sigma);
        }
        AiCallContext ctx = new AiCallContext(TenantContext.getTenantId(), "spc",
                values == null ? 0 : values.size());
        guard.check(ctx);
        try {
            Map<String, Object> resp = client.post()
                    .uri("/v1/ai/spc/analyze")
                    .header("X-Dev-Claims", devClaims)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp == null) {
                throw new AiGatewayException("Réponse vide de la passerelle IA (SPC)");
            }
            guard.recordSuccess(ctx.tenantId());
            return resp;
        } catch (RestClientException e) {
            guard.recordFailure(ctx.tenantId());
            throw new AiGatewayException("Passerelle IA indisponible (SPC) : " + e.getMessage(), e);
        }
    }

    /**
     * Relaie une matrice multivariée vers la détection d'anomalies non-supervisée de
     * {@code ai-service} (§3.4, §12.1 : Isolation Forest ou reconstruction par ACP,
     * NumPy). Renvoie la réponse JSON brute (mappée par la couche application). Le tenant
     * provient du {@link TenantContext} (JWT), jamais du body.
     *
     * <p>Comme pour le SPC, l'analyse est statistique (pas un LLM) mais passe par le même
     * garde-fou ({@link AiGuard}, op « anomaly ») : cloisonnement débit/quota par tenant
     * sur la passerelle (OWASP LLM04, cohérence des chemins sortants).
     *
     * @param samples       matrice échantillons × features
     * @param method        {@code isolation_forest} ou {@code reconstruction}
     * @param contamination fraction d'anomalies attendue (0, 0.5]
     * @param threshold     seuil explicite sur le score (optionnel ; sinon quantile)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> detectAnomaly(
            List<List<Double>> samples, String method, Double contamination, Double threshold) {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        String devClaims = devClaims(UUID.fromString(TenantContext.getTenantId()));
        // Map.of refuse les valeurs null → construire la map en tenant compte des optionnels.
        Map<String, Object> body = new HashMap<>();
        body.put("samples", samples);
        if (method != null) {
            body.put("method", method);
        }
        if (contamination != null) {
            body.put("contamination", contamination);
        }
        if (threshold != null) {
            body.put("threshold", threshold);
        }
        AiCallContext ctx = new AiCallContext(TenantContext.getTenantId(), "anomaly",
                samples == null ? 0 : samples.size());
        guard.check(ctx);
        try {
            Map<String, Object> resp = client.post()
                    .uri("/v1/ai/anomaly/detect")
                    .header("X-Dev-Claims", devClaims)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp == null) {
                throw new AiGatewayException("Réponse vide de la passerelle IA (anomaly)");
            }
            guard.recordSuccess(ctx.tenantId());
            return resp;
        } catch (RestClientException e) {
            guard.recordFailure(ctx.tenantId());
            throw new AiGatewayException("Passerelle IA indisponible (anomaly) : " + e.getMessage(), e);
        }
    }

    /**
     * Relaie une série de mesures KPI vers la prévision de {@code ai-service} (§6.5, §12.1 :
     * lissage exponentiel Holt-Winters, NumPy). Renvoie la réponse JSON brute (mappée par la
     * couche application). Le tenant provient du {@link TenantContext} (JWT), jamais du body.
     *
     * <p>Comme pour le SPC/anomaly, le calcul est statistique (pas un LLM) mais passe par le
     * même garde-fou ({@link AiGuard}, op « forecast ») : cloisonnement débit/quota par tenant
     * (OWASP LLM04, cohérence des chemins sortants).
     *
     * @param values         série chronologique (≥ 4 points)
     * @param target         valeur cible à atteindre
     * @param horizon        nombre de périodes à projeter (1..60)
     * @param direction      {@code at_least} ou {@code at_most}
     * @param seasonalPeriod période saisonnière (optionnelle ; null = pas de saisonnalité)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> forecastKpi(
            List<Double> values, Double target, Integer horizon, String direction,
            Integer seasonalPeriod) {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        String devClaims = devClaims(UUID.fromString(TenantContext.getTenantId()));
        // Map.of refuse les valeurs null → construire la map en tenant compte des optionnels.
        Map<String, Object> body = new HashMap<>();
        body.put("values", values);
        body.put("target", target);
        if (horizon != null) {
            body.put("horizon", horizon);
        }
        if (direction != null) {
            body.put("direction", direction);
        }
        if (seasonalPeriod != null) {
            body.put("seasonal_period", seasonalPeriod);
        }
        AiCallContext ctx = new AiCallContext(TenantContext.getTenantId(), "forecast",
                values == null ? 0 : values.size());
        guard.check(ctx);
        try {
            Map<String, Object> resp = client.post()
                    .uri("/v1/ai/predict/kpi")
                    .header("X-Dev-Claims", devClaims)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp == null) {
                throw new AiGatewayException("Réponse vide de la passerelle IA (forecast)");
            }
            guard.recordSuccess(ctx.tenantId());
            return resp;
        } catch (RestClientException e) {
            guard.recordFailure(ctx.tenantId());
            throw new AiGatewayException("Passerelle IA indisponible (forecast) : " + e.getMessage(), e);
        }
    }

    /**
     * Relaie une demande d'explication d'anomalie vers {@code ai-service} (§12.3 :
     * Kernel SHAP attribuant le score d'Isolation Forest aux features, NumPy). Renvoie la
     * réponse JSON brute (mappée par la couche application). Tenant via {@link TenantContext}
     * (JWT), jamais du body. Même garde-fou ({@link AiGuard}, op « anomaly-explain »).
     *
     * @param samples matrice échantillons × features
     * @param index   index (0-based) de l'échantillon à expliquer
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> explainAnomaly(List<List<Double>> samples, Integer index) {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        String devClaims = devClaims(UUID.fromString(TenantContext.getTenantId()));
        Map<String, Object> body = new HashMap<>();
        body.put("samples", samples);
        body.put("index", index);
        AiCallContext ctx = new AiCallContext(TenantContext.getTenantId(), "anomaly-explain",
                samples == null ? 0 : samples.size());
        guard.check(ctx);
        try {
            Map<String, Object> resp = client.post()
                    .uri("/v1/ai/anomaly/explain")
                    .header("X-Dev-Claims", devClaims)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp == null) {
                throw new AiGatewayException("Réponse vide de la passerelle IA (anomaly-explain)");
            }
            guard.recordSuccess(ctx.tenantId());
            return resp;
        } catch (RestClientException e) {
            guard.recordFailure(ctx.tenantId());
            throw new AiGatewayException("Passerelle IA indisponible (anomaly-explain) : " + e.getMessage(), e);
        }
    }

    /**
     * Relaie une liste de textes de non-conformités vers le clustering de {@code ai-service}
     * (§4.3, §12.1 : TF-IDF + DBSCAN densité, NumPy). Renvoie la réponse JSON brute (mappée
     * par la couche application). Le tenant provient du {@link TenantContext} (JWT), jamais du
     * body. Même garde-fou ({@link AiGuard}, op « nccluster ») que les autres chemins IA.
     *
     * @param texts      textes de NC (≥ 2)
     * @param threshold  similarité cosinus minimale du voisinage (optionnel)
     * @param minSamples taille minimale du voisinage d'un point-cœur (optionnel)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> clusterNc(List<String> texts, Double threshold, Integer minSamples) {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        String devClaims = devClaims(UUID.fromString(TenantContext.getTenantId()));
        Map<String, Object> body = new HashMap<>();
        body.put("texts", texts);
        if (threshold != null) {
            body.put("threshold", threshold);
        }
        if (minSamples != null) {
            body.put("min_samples", minSamples);
        }
        AiCallContext ctx = new AiCallContext(TenantContext.getTenantId(), "nccluster",
                texts == null ? 0 : texts.size());
        guard.check(ctx);
        try {
            Map<String, Object> resp = client.post()
                    .uri("/v1/ai/predict/nc-clusters")
                    .header("X-Dev-Claims", devClaims)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp == null) {
                throw new AiGatewayException("Réponse vide de la passerelle IA (nccluster)");
            }
            guard.recordSuccess(ctx.tenantId());
            return resp;
        } catch (RestClientException e) {
            guard.recordFailure(ctx.tenantId());
            throw new AiGatewayException("Passerelle IA indisponible (nccluster) : " + e.getMessage(), e);
        }
    }

    /**
     * Relaie un lot de réclamations vers l'analyse NLP de {@code ai-service} (§4.9, §12.1 :
     * sentiment lexical + classification + criticité). Renvoie la réponse JSON brute (mappée
     * par la couche application). Tenant via {@link TenantContext} (JWT), jamais du body. Même
     * garde-fou ({@link AiGuard}, op « complaint-nlp »).
     *
     * @param texts      réclamations à analyser
     * @param categories taxonomie optionnelle {catégorie: [termes-graines]} (null = défaut)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> analyzeComplaints(
            List<String> texts, Map<String, List<String>> categories) {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        String devClaims = devClaims(UUID.fromString(TenantContext.getTenantId()));
        Map<String, Object> body = new HashMap<>();
        body.put("texts", texts);
        if (categories != null) {
            body.put("categories", categories);
        }
        AiCallContext ctx = new AiCallContext(TenantContext.getTenantId(), "complaint-nlp",
                texts == null ? 0 : texts.size());
        guard.check(ctx);
        try {
            Map<String, Object> resp = client.post()
                    .uri("/v1/ai/complaints/analyze")
                    .header("X-Dev-Claims", devClaims)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp == null) {
                throw new AiGatewayException("Réponse vide de la passerelle IA (complaint-nlp)");
            }
            guard.recordSuccess(ctx.tenantId());
            return resp;
        } catch (RestClientException e) {
            guard.recordFailure(ctx.tenantId());
            throw new AiGatewayException("Passerelle IA indisponible (complaint-nlp) : " + e.getMessage(), e);
        }
    }

    /**
     * Relaie une demande d'audit blanc vers {@code ai-service} (Standards Hub §8.4
     * onglet 7) : l'IA génère 30-100 questions ciblées sur les clauses à risque et
     * confronte chaque clause aux preuves disponibles (gap analysis). Renvoie la
     * réponse JSON brute (mappée par la couche application). Le tenant provient du
     * {@link TenantContext} (JWT), jamais du body (§18.2 #2/#4). Même garde-fou
     * ({@link AiGuard}, op « mock-audit ») que les autres chemins IA.
     *
     * @param body corps de requête pré-construit (norme + clauses à risque) ; le
     *             nombre de clauses sert d'unité de débit pour le garde-fou
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> mockAudit(Map<String, Object> body, int clauseCount) {
        if (!TenantContext.hasTenant()) {
            throw new MissingTenantContextException();
        }
        String devClaims = devClaims(UUID.fromString(TenantContext.getTenantId()));
        AiCallContext ctx = new AiCallContext(TenantContext.getTenantId(), "mock-audit",
                clauseCount);
        guard.check(ctx);
        try {
            Map<String, Object> resp = client.post()
                    .uri("/v1/ai/standards/mock-audit")
                    .header("X-Dev-Claims", devClaims)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp == null) {
                throw new AiGatewayException("Réponse vide de la passerelle IA (mock-audit)");
            }
            guard.recordSuccess(ctx.tenantId());
            return resp;
        } catch (RestClientException e) {
            guard.recordFailure(ctx.tenantId());
            throw new AiGatewayException("Passerelle IA indisponible (mock-audit) : " + e.getMessage(), e);
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
