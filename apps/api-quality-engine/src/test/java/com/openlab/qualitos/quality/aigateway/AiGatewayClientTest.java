package com.openlab.qualitos.quality.aigateway;

import com.openlab.qualitos.quality.ai.guard.AiGuard;
import com.openlab.qualitos.quality.ai.guard.AiGuardProperties;
import com.openlab.qualitos.quality.ai.guard.TokenBucketAiGuard;
import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste le vrai {@link AiGatewayClient} contre un serveur HTTP local (JDK), sans
 * dépendance ni refactor : couvre les chemins succès / réponse vide / erreur HTTP /
 * tenant manquant pour {@code complete} et {@code askNlq}.
 */
class AiGatewayClientTest {

    private HttpServer server;
    private int port;
    private static final UUID TENANT = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
        port = server.getAddress().getPort();
        TenantContext.setTenantId(TENANT.toString());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        TenantContext.clear();
    }

    /** Garde-fou par défaut (débit large) : neutre pour les appels uniques de chaque test. */
    private static AiGuard newGuard() {
        return new TokenBucketAiGuard(new AiGuardProperties());
    }

    /** Enregistre une réponse canned et renvoie un client pointé sur le serveur. */
    private AiGatewayClient clientReturning(int status, String body) {
        server.createContext("/", exchange -> {
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, b.length == 0 ? -1 : b.length);
            if (b.length > 0) {
                exchange.getResponseBody().write(b);
            }
            exchange.close();
        });
        return new AiGatewayClient("http://localhost:" + port, 2000, 5000, newGuard());
    }

    @Test
    void complete_success_parsesResult() {
        AiGatewayClient c = clientReturning(200,
                "{\"text\":\"bonjour\",\"provider\":\"ollama\",\"tokens_used\":7,\"latency_ms\":123}");
        AiCompletionResult r = c.complete("système", "utilisateur", 50);
        assertThat(r.text()).isEqualTo("bonjour");
        assertThat(r.provider()).isEqualTo("ollama");
        assertThat(r.tokensUsed()).isEqualTo(7);
        assertThat(r.latencyMs()).isEqualTo(123);
    }

    @Test
    void complete_missingTextField_throws() {
        AiGatewayClient c = clientReturning(200, "{\"provider\":\"ollama\"}");
        assertThatThrownBy(() -> c.complete("s", "u", 10))
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    void complete_emptyBody_throws() {
        AiGatewayClient c = clientReturning(200, "");   // body null côté RestClient
        assertThatThrownBy(() -> c.complete("s", "u", 10))
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    void complete_serverError_throws() {
        AiGatewayClient c = clientReturning(500, "boom");
        assertThatThrownBy(() -> c.complete("s", "u", 10))
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    void complete_missingTenant_throws() {
        TenantContext.clear();
        AiGatewayClient c = new AiGatewayClient("http://localhost:" + port, 2000, 5000, newGuard());
        assertThatThrownBy(() -> c.complete("s", "u", 10))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void askNlq_success_returnsRawMap() {
        AiGatewayClient c = clientReturning(200,
                "{\"sql\":{\"sql\":\"SELECT 1\",\"tenant_filter_applied\":true},\"rows\":[],\"row_count\":0}");
        Map<String, Object> resp = c.askNlq("Combien de CAPA ?", 50);
        assertThat(resp).containsKey("sql");
        assertThat(resp.get("row_count")).isEqualTo(0);
    }

    @Test
    void askNlq_emptyBody_throws() {
        AiGatewayClient c = clientReturning(200, "");
        assertThatThrownBy(() -> c.askNlq("q", 10))
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    void askNlq_serverError_throws() {
        AiGatewayClient c = clientReturning(503, "down");
        assertThatThrownBy(() -> c.askNlq("q", 10))
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    void askNlq_missingTenant_throws() {
        TenantContext.clear();
        AiGatewayClient c = new AiGatewayClient("http://localhost:" + port, 2000, 5000, newGuard());
        assertThatThrownBy(() -> c.askNlq("q", 10))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void detectSpc_success_returnsRawMap() {
        AiGatewayClient c = clientReturning(200,
                "{\"n\":3,\"out_of_control\":true,\"limits\":{\"ucl\":3.5},\"violations\":[]}");
        Map<String, Object> resp = c.detectSpc(List.of(1.0, 2.0, 3.0), null, null);
        assertThat(resp).containsKey("limits");
        assertThat(resp.get("out_of_control")).isEqualTo(true);
    }

    @Test
    void detectSpc_emptyBody_throws() {
        AiGatewayClient c = clientReturning(200, "");
        assertThatThrownBy(() -> c.detectSpc(List.of(1.0), null, null))
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    void detectSpc_serverError_throws() {
        AiGatewayClient c = clientReturning(500, "boom");
        assertThatThrownBy(() -> c.detectSpc(List.of(1.0), null, null))
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    void detectSpc_missingTenant_throws() {
        TenantContext.clear();
        AiGatewayClient c = new AiGatewayClient("http://localhost:" + port, 2000, 5000, newGuard());
        assertThatThrownBy(() -> c.detectSpc(List.of(1.0), null, null))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void detectAnomaly_success_returnsRawMap() {
        AiGatewayClient c = clientReturning(200,
                "{\"n\":3,\"n_features\":2,\"method\":\"isolation_forest\","
                        + "\"anomaly_count\":1,\"has_anomalies\":true,\"points\":[]}");
        Map<String, Object> resp = c.detectAnomaly(
                List.of(List.of(1.0, 2.0), List.of(50.0, -50.0)), "isolation_forest", 0.1, null);
        assertThat(resp).containsKey("points");
        assertThat(resp.get("has_anomalies")).isEqualTo(true);
    }

    @Test
    void detectAnomaly_emptyBody_throws() {
        AiGatewayClient c = clientReturning(200, "");
        assertThatThrownBy(() -> c.detectAnomaly(List.of(List.of(1.0)), null, null, null))
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    void detectAnomaly_serverError_throws() {
        AiGatewayClient c = clientReturning(500, "boom");
        assertThatThrownBy(() -> c.detectAnomaly(List.of(List.of(1.0)), "reconstruction", 0.2, 0.7))
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    void detectAnomaly_missingTenant_throws() {
        TenantContext.clear();
        AiGatewayClient c = new AiGatewayClient("http://localhost:" + port, 2000, 5000, newGuard());
        assertThatThrownBy(() -> c.detectAnomaly(List.of(List.of(1.0)), null, null, null))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void forecastKpi_success_returnsRawMap() {
        AiGatewayClient c = clientReturning(200,
                "{\"n\":10,\"slope\":2.0,\"intercept\":28.0,\"model\":\"holt_linear\","
                        + "\"seasonal_period\":0,\"probability\":0.97,\"confidence\":\"high\",\"points\":[]}");
        Map<String, Object> resp = c.forecastKpi(
                List.of(10.0, 12.0, 14.0, 16.0), 35.0, 6, "at_least", null);
        assertThat(resp).containsKey("points");
        assertThat(resp.get("model")).isEqualTo("holt_linear");
    }

    @Test
    void forecastKpi_passesSeasonalPeriod() {
        AiGatewayClient c = clientReturning(200,
                "{\"model\":\"holt_winters_additive\",\"seasonal_period\":4,\"points\":[]}");
        Map<String, Object> resp = c.forecastKpi(
                List.of(10.0, 14.0, 9.0, 13.0, 11.0, 15.0, 10.0, 14.0), 30.0, 4, "at_most", 4);
        assertThat(resp.get("seasonal_period")).isEqualTo(4);
    }

    @Test
    void forecastKpi_emptyBody_throws() {
        AiGatewayClient c = clientReturning(200, "");
        assertThatThrownBy(() -> c.forecastKpi(List.of(1.0, 2.0, 3.0, 4.0), 10.0, null, null, null))
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    void forecastKpi_serverError_throws() {
        AiGatewayClient c = clientReturning(500, "boom");
        assertThatThrownBy(() -> c.forecastKpi(List.of(1.0, 2.0, 3.0, 4.0), 10.0, 6, "at_least", null))
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    void forecastKpi_missingTenant_throws() {
        TenantContext.clear();
        AiGatewayClient c = new AiGatewayClient("http://localhost:" + port, 2000, 5000, newGuard());
        assertThatThrownBy(() -> c.forecastKpi(List.of(1.0, 2.0, 3.0, 4.0), 10.0, null, null, null))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void clusterNc_success_returnsRawMap() {
        AiGatewayClient c = clientReturning(200,
                "{\"n\":5,\"clustered_ratio\":0.8,\"method\":\"dbscan\",\"clusters\":[],\"noise_indices\":[4]}");
        Map<String, Object> resp = c.clusterNc(
                List.of("fuite huile presse", "fuite huile presse ligne"), null, null);
        assertThat(resp.get("method")).isEqualTo("dbscan");
        assertThat(resp).containsKey("noise_indices");
    }

    @Test
    void clusterNc_passesThresholdAndMinSamples() {
        AiGatewayClient c = clientReturning(200, "{\"method\":\"dbscan\",\"clusters\":[]}");
        Map<String, Object> resp = c.clusterNc(List.of("a b", "a c"), 0.5, 3);
        assertThat(resp).containsKey("clusters");
    }

    @Test
    void clusterNc_serverError_throws() {
        AiGatewayClient c = clientReturning(500, "boom");
        assertThatThrownBy(() -> c.clusterNc(List.of("a b", "a c"), null, null))
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    void clusterNc_missingTenant_throws() {
        TenantContext.clear();
        AiGatewayClient c = new AiGatewayClient("http://localhost:" + port, 2000, 5000, newGuard());
        assertThatThrownBy(() -> c.clusterNc(List.of("a b", "a c"), null, null))
                .isInstanceOf(MissingTenantContextException.class);
    }

    @Test
    void explainAnomaly_success_returnsRawMap() {
        AiGatewayClient c = clientReturning(200,
                "{\"index\":2,\"method\":\"isolation_forest\",\"score\":0.82,\"base_value\":0.5,"
                        + "\"contributions\":[{\"feature\":0,\"value\":50.0,\"contribution\":0.2}]}");
        Map<String, Object> resp = c.explainAnomaly(
                List.of(List.of(1.0, 2.0), List.of(50.0, -50.0)), 1);
        assertThat(resp.get("method")).isEqualTo("isolation_forest");
        assertThat(resp).containsKey("contributions");
    }

    @Test
    void explainAnomaly_serverError_throws() {
        AiGatewayClient c = clientReturning(500, "boom");
        assertThatThrownBy(() -> c.explainAnomaly(List.of(List.of(1.0, 2.0)), 0))
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    void explainAnomaly_missingTenant_throws() {
        TenantContext.clear();
        AiGatewayClient c = new AiGatewayClient("http://localhost:" + port, 2000, 5000, newGuard());
        assertThatThrownBy(() -> c.explainAnomaly(List.of(List.of(1.0, 2.0)), 0))
                .isInstanceOf(MissingTenantContextException.class);
    }
}
