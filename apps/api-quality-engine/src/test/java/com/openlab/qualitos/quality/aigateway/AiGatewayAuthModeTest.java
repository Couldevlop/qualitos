package com.openlab.qualitos.quality.aigateway;

import com.openlab.qualitos.quality.ai.guard.AiGuard;
import com.openlab.qualitos.quality.ai.guard.AiGuardProperties;
import com.openlab.qualitos.quality.ai.guard.TokenBucketAiGuard;
import com.openlab.qualitos.quality.common.TenantContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Mode d'authentification des appels engine → ai-service.
 *
 * <p>Ce test existe parce que le défaut qu'il couvre est passé inaperçu jusqu'au premier
 * déploiement réel : la passerelle posait {@code X-Dev-Claims} en dur sur chacun de ses
 * onze points d'appel. Tant que l'ai-service tournait en mode développement, tout
 * fonctionnait ; dès qu'il a validé les jetons pour de bon, toutes les fonctions d'IA ont
 * répondu 502. On vérifie donc ici ce qui part réellement sur le réseau, pas ce que la
 * documentation annonce.
 */
class AiGatewayAuthModeTest {

    private HttpServer server;
    private int port;
    private static final UUID TENANT = UUID.randomUUID();

    private final AtomicReference<String> devClaimsHeader = new AtomicReference<>();
    private final AtomicReference<String> authorizationHeader = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
        port = server.getAddress().getPort();
        TenantContext.setTenantId(TENANT.toString());

        // Point d'émission de jeton (Keycloak simulé).
        server.createContext("/token", exchange -> {
            byte[] b = "{\"access_token\":\"jeton-de-service\",\"expires_in\":300}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, b.length);
            exchange.getResponseBody().write(b);
            exchange.close();
        });

        // ai-service simulé : capture les en-têtes d'authentification reçus.
        server.createContext("/v1/ai/anomaly/detect", exchange -> {
            devClaimsHeader.set(first(exchange.getRequestHeaders().get("X-Dev-Claims")));
            authorizationHeader.set(first(exchange.getRequestHeaders().get("Authorization")));
            byte[] b = "{\"anomalies\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, b.length);
            exchange.getResponseBody().write(b);
            exchange.close();
        });
    }

    private static String first(List<String> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        TenantContext.clear();
    }

    private static AiGuard guard() {
        return new TokenBucketAiGuard(new AiGuardProperties());
    }

    private AiServiceTokenProvider tokenProvider() {
        return new AiServiceTokenProvider("http://localhost:" + port + "/token",
                "api-quality-engine-ai", "s3cr3t", "", 30, 1000, 2000);
    }

    private AiGatewayClient client(String mode) {
        return new AiGatewayClient("http://localhost:" + port, 2000, 5000, mode, guard(), tokenProvider());
    }

    @Test
    @DisplayName("mode bearer : envoie Authorization et JAMAIS X-Dev-Claims")
    void modeBearer() {
        client("bearer").detectAnomaly(List.of(List.of(1.0, 2.0), List.of(30.0, 40.0)), null, null, null);

        assertThat(authorizationHeader.get()).isEqualTo("Bearer jeton-de-service");
        // Le point essentiel : l'en-tête déclaratif ne doit plus partir du tout.
        // Le laisser en plus du jeton offrirait un chemin de repli exploitable.
        assertThat(devClaimsHeader.get()).isNull();
    }

    @Test
    @DisplayName("mode dev-claims : envoie X-Dev-Claims et aucun jeton")
    void modeDevClaims() {
        client("dev-claims").detectAnomaly(List.of(List.of(1.0, 2.0)), null, null, null);

        assertThat(devClaimsHeader.get()).contains(TENANT.toString());
        assertThat(authorizationHeader.get()).isNull();
    }

    @Test
    @DisplayName("mode absent : reste en dev-claims, pour ne rien casser en local")
    void modeParDefaut() {
        client("").detectAnomaly(List.of(List.of(1.0, 2.0)), null, null, null);
        assertThat(devClaimsHeader.get()).isNotNull();
        assertThat(authorizationHeader.get()).isNull();
    }

    @Test
    @DisplayName("mode inconnu : refuse de démarrer au lieu de se dégrader")
    void modeInconnu() {
        // Retomber en silence sur dev-claims rouvrirait exactement la faille que ce
        // mode ferme : mieux vaut un démarrage impossible qu'une sécurité illusoire.
        assertThatThrownBy(() -> client("oui"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("qualitos.ai.auth");
    }

    @Test
    @DisplayName("mode bearer sans configuration de jeton : échoue, ne se rabat pas")
    void bearerSansConfiguration() {
        AiGatewayClient c = new AiGatewayClient("http://localhost:" + port, 2000, 5000, "bearer",
                guard(), new AiServiceTokenProvider("", "", "", "", 30, 1000, 1000));

        assertThatThrownBy(() -> c.detectAnomaly(List.of(List.of(1.0)), null, null, null))
                .isInstanceOf(AiGatewayException.class);
        // Aucun appel n'a atteint l'ai-service : le refus est en amont.
        assertThat(devClaimsHeader.get()).isNull();
        assertThat(authorizationHeader.get()).isNull();
    }
}
