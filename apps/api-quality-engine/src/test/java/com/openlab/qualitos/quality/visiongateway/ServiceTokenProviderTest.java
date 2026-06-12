package com.openlab.qualitos.quality.visiongateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste le vrai {@link ServiceTokenProvider} contre un token endpoint simulé (serveur HTTP
 * JDK local, même schéma que {@link VisionGatewayClientTest}) : forme de la requête
 * client_credentials, cache jusqu'à expiration moins la marge, rafraîchissement à
 * l'expiration, fail-closed (config incomplète, erreur HTTP, réponse sans access_token).
 */
class ServiceTokenProviderTest {

    private HttpServer server;
    private String tokenUri;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
        tokenUri = "http://localhost:" + server.getAddress().getPort() + "/token";
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    /** Monte un endpoint /token : capture le corps de la requête, renvoie {@code body}. */
    private AtomicInteger tokenEndpoint(int status, String body, AtomicReference<String> lastForm) {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/token", exchange -> {
            calls.incrementAndGet();
            ByteArrayOutputStream in = new ByteArrayOutputStream();
            exchange.getRequestBody().transferTo(in);
            if (lastForm != null) {
                lastForm.set(URLDecoder.decode(in.toString(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
            }
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, b.length == 0 ? -1 : b.length);
            if (b.length > 0) {
                exchange.getResponseBody().write(b);
            }
            exchange.close();
        });
        return calls;
    }

    private ServiceTokenProvider provider(long marginSeconds) {
        return new ServiceTokenProvider(tokenUri, "api-quality-engine-vision", "s3cr3t",
                "vision", marginSeconds, 1000, 2000);
    }

    @Test
    void getToken_postsClientCredentialsForm_andReturnsToken() {
        AtomicReference<String> form = new AtomicReference<>();
        tokenEndpoint(200, "{\"access_token\":\"tok-1\",\"expires_in\":300}", form);
        String token = provider(30).getToken();
        assertThat(token).isEqualTo("tok-1");
        assertThat(form.get())
                .contains("grant_type=client_credentials")
                .contains("client_id=api-quality-engine-vision")
                .contains("client_secret=s3cr3t")
                .contains("scope=vision");
    }

    @Test
    void getToken_scopeOmittedWhenBlank() {
        AtomicReference<String> form = new AtomicReference<>();
        tokenEndpoint(200, "{\"access_token\":\"tok\",\"expires_in\":300}", form);
        ServiceTokenProvider p = new ServiceTokenProvider(tokenUri, "cid", "sec", "", 30, 1000, 2000);
        assertThat(p.getToken()).isEqualTo("tok");
        assertThat(form.get()).doesNotContain("scope=");
    }

    @Test
    void getToken_cachesUntilExpiryMinusMargin() {
        AtomicInteger calls = tokenEndpoint(200, "{\"access_token\":\"tok-cache\",\"expires_in\":300}", null);
        ServiceTokenProvider p = provider(30);
        assertThat(p.getToken()).isEqualTo("tok-cache");
        assertThat(p.getToken()).isEqualTo("tok-cache");
        assertThat(p.getToken()).isEqualTo("tok-cache");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void getToken_refetchesWhenTokenTooShortLived() {
        // expires_in (10 s) <= marge (30 s) → durée utilisable nulle : chaque appel rafraîchit.
        AtomicInteger calls = tokenEndpoint(200, "{\"access_token\":\"tok-court\",\"expires_in\":10}", null);
        ServiceTokenProvider p = provider(30);
        assertThat(p.getToken()).isEqualTo("tok-court");
        assertThat(p.getToken()).isEqualTo("tok-court");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void getToken_missingConfig_failsClosed() {
        ServiceTokenProvider p = new ServiceTokenProvider("", "", "", "", 30, 1000, 2000);
        assertThatThrownBy(p::getToken)
                .isInstanceOf(VisionUnavailableException.class)
                .hasMessageContaining("incomplète");
    }

    @Test
    void getToken_httpError_failsClosed_withoutLeakingSecret() {
        tokenEndpoint(401, "{\"error\":\"unauthorized_client\"}", null);
        assertThatThrownBy(() -> provider(30).getToken())
                .isInstanceOf(VisionUnavailableException.class)
                // Le message ne doit JAMAIS contenir le secret du client (§22-9).
                .satisfies(e -> assertThat(e.getMessage()).doesNotContain("s3cr3t"));
    }

    @Test
    void getToken_missingAccessToken_failsClosed() {
        tokenEndpoint(200, "{\"token_type\":\"Bearer\",\"expires_in\":300}", null);
        assertThatThrownBy(() -> provider(30).getToken())
                .isInstanceOf(VisionUnavailableException.class)
                .hasMessageContaining("access_token");
    }

    @Test
    void getToken_unreachableEndpoint_failsClosed() throws Exception {
        server.stop(0); // plus personne n'écoute → connexion refusée
        assertThatThrownBy(() -> provider(30).getToken())
                .isInstanceOf(VisionUnavailableException.class);
    }
}
