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
}
