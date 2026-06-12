package com.openlab.qualitos.quality.visiongateway;

import com.openlab.qualitos.quality.common.MissingTenantContextException;
import com.openlab.qualitos.quality.common.TenantContext;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste le vrai {@link VisionGatewayClient} contre un serveur HTTP local (JDK), sans
 * dépendance ni refactor : couvre succès → mapping DTO, flag OFF → 503, réponse vide →
 * 502, erreur HTTP en aval → 503, tenant manquant, et les trois modes d'auth interne
 * (dev-claims / bearer / none, ADR 0021). Même schéma que {@code AiGatewayClientTest}.
 */
class VisionGatewayClientTest {

    private HttpServer server;
    private int port;
    private static final UUID TENANT = UUID.randomUUID();

    /** Provider jamais sollicité (modes none/dev-claims) — échoue si appelé par erreur. */
    private static ServiceTokenProvider unusedTokenProvider() {
        return new ServiceTokenProvider("", "", "", "", 30, 1000, 1000) {
            @Override
            public String getToken() {
                throw new AssertionError("getToken() ne doit pas être appelé dans ce mode");
            }
        };
    }

    /** Provider stub renvoyant un jeton fixe (mode bearer). */
    private static ServiceTokenProvider fixedTokenProvider(String token) {
        return new ServiceTokenProvider("", "", "", "", 30, 1000, 1000) {
            @Override
            public String getToken() {
                return token;
            }
        };
    }

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

    private static final String OK_BODY =
            "{\"image_sha256\":\"z\",\"width\":2,\"height\":2,"
                    + "\"score\":{\"seiri\":0,\"seiton\":0,\"seiso\":0,\"seiketsu\":0,"
                    + "\"shitsuke\":0,\"overall\":0},\"findings\":[]}";

    /** Enregistre une réponse canned, capture les en-têtes reçus dans {@code seenHeaders}. */
    private void respondWith(int status, String body, Map<String, String> seenHeaders) {
        server.createContext("/", exchange -> {
            if (seenHeaders != null) {
                exchange.getRequestHeaders().forEach((k, v) -> seenHeaders.put(k, v.get(0)));
            }
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, b.length == 0 ? -1 : b.length);
            if (b.length > 0) {
                exchange.getResponseBody().write(b);
            }
            exchange.close();
        });
    }

    /** Enregistre une réponse canned et renvoie un client ACTIVÉ pointé sur le serveur. */
    private VisionGatewayClient enabledClientReturning(int status, String body) {
        respondWith(status, body, null);
        return new VisionGatewayClient(true, "http://localhost:" + port, 2000, 5000,
                "dev-claims", unusedTokenProvider());
    }

    private static byte[] png() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    }

    @Test
    void analyze_success_mapsDto() {
        VisionGatewayClient c = enabledClientReturning(200,
                "{\"image_sha256\":\"abc\",\"width\":1280,\"height\":720,"
                        + "\"score\":{\"seiri\":80,\"seiton\":70,\"seiso\":60,\"seiketsu\":90,"
                        + "\"shitsuke\":50,\"overall\":70},"
                        + "\"findings\":[{\"pillar\":\"SEIRI\",\"description\":\"clutter\","
                        + "\"severity\":\"HIGH\",\"confidence\":0.9,\"bbox\":[1,2,3,4]}]}");
        VisionDto.VisionAnalysis r = c.analyze("image/png", "shop.png", png());
        assertThat(r.imageSha256()).isEqualTo("abc");
        assertThat(r.width()).isEqualTo(1280);
        assertThat(r.height()).isEqualTo(720);
        assertThat(r.score().overall()).isEqualTo(70);
        assertThat(r.score().seiketsu()).isEqualTo(90);
        assertThat(r.findings()).hasSize(1);
        assertThat(r.findings().get(0).pillar()).isEqualTo("SEIRI");
        assertThat(r.findings().get(0).confidence()).isEqualTo(0.9);
        assertThat(r.findings().get(0).bbox()).containsExactly(1, 2, 3, 4);
    }

    @Test
    void analyze_findingWithoutBbox_mapsNull() {
        VisionGatewayClient c = enabledClientReturning(200,
                "{\"image_sha256\":\"a\",\"width\":1,\"height\":1,"
                        + "\"score\":{\"seiri\":1,\"seiton\":1,\"seiso\":1,\"seiketsu\":1,"
                        + "\"shitsuke\":1,\"overall\":1},"
                        + "\"findings\":[{\"pillar\":\"SEISO\",\"description\":\"d\","
                        + "\"severity\":\"LOW\",\"confidence\":0.1,\"bbox\":null}]}");
        VisionDto.VisionAnalysis r = c.analyze("image/png", null, png());
        assertThat(r.findings().get(0).bbox()).isNull();
    }

    @Test
    void analyze_flagOff_throwsUnavailable() {
        VisionGatewayClient c = new VisionGatewayClient(false, "http://localhost:" + port, 2000, 5000,
                "dev-claims", unusedTokenProvider());
        assertThatThrownBy(() -> c.analyze("image/png", "x.png", png()))
                .isInstanceOf(VisionUnavailableException.class);
    }

    @Test
    void analyze_emptyBody_throwsGateway() {
        VisionGatewayClient c = enabledClientReturning(200, "");
        assertThatThrownBy(() -> c.analyze("image/png", "x.png", png()))
                .isInstanceOf(VisionGatewayException.class);
    }

    @Test
    void analyze_serverError_throwsUnavailable() {
        VisionGatewayClient c = enabledClientReturning(500, "boom");
        assertThatThrownBy(() -> c.analyze("image/png", "x.png", png()))
                .isInstanceOf(VisionUnavailableException.class);
    }

    @Test
    void analyze_clientError_throwsUnavailable() {
        VisionGatewayClient c = enabledClientReturning(400, "{\"title\":\"bad image\"}");
        assertThatThrownBy(() -> c.analyze("image/png", "x.png", png()))
                .isInstanceOf(VisionUnavailableException.class);
    }

    @Test
    void analyze_missingTenant_throws() {
        TenantContext.clear();
        VisionGatewayClient c = new VisionGatewayClient(true, "http://localhost:" + port, 2000, 5000,
                "dev-claims", unusedTokenProvider());
        assertThatThrownBy(() -> c.analyze("image/png", "x.png", png()))
                .isInstanceOf(MissingTenantContextException.class);
    }

    // --- Modes d'auth interne (ADR 0021) ---

    @Test
    void analyze_devClaimsMode_sendsDevClaimsHeaderWithTenant() {
        Map<String, String> seen = new ConcurrentHashMap<>();
        respondWith(200, OK_BODY, seen);
        VisionGatewayClient c = new VisionGatewayClient(true, "http://localhost:" + port, 2000, 5000,
                "dev-claims", unusedTokenProvider());
        c.analyze("image/png", "x.png", png());
        assertThat(seen.get("X-dev-claims")).contains("\"tid\":\"" + TENANT + "\"");
        assertThat(seen).doesNotContainKey("Authorization");
    }

    @Test
    void analyze_noneMode_sendsNoAuthHeaders() {
        Map<String, String> seen = new ConcurrentHashMap<>();
        respondWith(200, OK_BODY, seen);
        VisionGatewayClient c = new VisionGatewayClient(true, "http://localhost:" + port, 2000, 5000,
                "none", unusedTokenProvider());
        VisionDto.VisionAnalysis r = c.analyze("image/png", "x.png", png());
        assertThat(r.imageSha256()).isEqualTo("z");
        assertThat(r.findings()).isEmpty();
        assertThat(seen).doesNotContainKey("Authorization");
        assertThat(seen).doesNotContainKey("X-dev-claims");
    }

    @Test
    void analyze_bearerMode_sendsAuthorizationAndTenantHeader() {
        Map<String, String> seen = new ConcurrentHashMap<>();
        respondWith(200, OK_BODY, seen);
        VisionGatewayClient c = new VisionGatewayClient(true, "http://localhost:" + port, 2000, 5000,
                "bearer", fixedTokenProvider("jeton-de-service"));
        c.analyze("image/png", "x.png", png());
        assertThat(seen.get("Authorization")).isEqualTo("Bearer jeton-de-service");
        assertThat(seen.get("X-tenant-id")).isEqualTo(TENANT.toString());
        assertThat(seen).doesNotContainKey("X-dev-claims");
    }

    @Test
    void analyze_bearerMode_tokenFailure_failsClosedWithoutCallingService() {
        // Provider RÉEL avec config vide → VisionUnavailableException (503), AVANT tout
        // appel réseau au service vision : pas de repli silencieux vers dev-claims.
        Map<String, String> seen = new ConcurrentHashMap<>();
        respondWith(200, OK_BODY, seen);
        VisionGatewayClient c = new VisionGatewayClient(true, "http://localhost:" + port, 2000, 5000,
                "bearer", new ServiceTokenProvider("", "", "", "", 30, 1000, 1000));
        assertThatThrownBy(() -> c.analyze("image/png", "x.png", png()))
                .isInstanceOf(VisionUnavailableException.class)
                .hasMessageContaining("incomplète");
        assertThat(seen).isEmpty();
    }

    @Test
    void constructor_unknownAuthMode_failsFast() {
        assertThatThrownBy(() -> new VisionGatewayClient(true, "http://localhost:" + port, 2000, 5000,
                "magic", unusedTokenProvider()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("qualitos.vision.auth");
    }
}
