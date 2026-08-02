package com.openlab.qualitos.quality.aigateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
 * Teste le vrai {@link AiServiceTokenProvider} contre un point d'émission simulé (serveur
 * HTTP du JDK, même schéma que {@code visiongateway.ServiceTokenProviderTest}).
 *
 * <p>L'essentiel porte sur les comportements de REFUS. Ce mécanisme n'existe que pour
 * fermer une faille — la passerelle envoyait un en-tête déclaratif que n'importe quel
 * appelant pouvait forger — et sa valeur tient entièrement à ce qu'il ne se dégrade jamais
 * en silence lorsque la configuration est incomplète.
 */
class AiServiceTokenProviderTest {

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

    /** Monte un point /token qui renvoie les corps fournis, l'un après l'autre. */
    private AtomicInteger tokenEndpoint(int status, AtomicReference<String> lastForm, String... bodies) {
        AtomicInteger calls = new AtomicInteger();
        server.createContext("/token", exchange -> {
            int n = calls.getAndIncrement();
            ByteArrayOutputStream in = new ByteArrayOutputStream();
            exchange.getRequestBody().transferTo(in);
            if (lastForm != null) {
                lastForm.set(URLDecoder.decode(in.toString(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
            }
            String chosen = bodies[Math.min(n, bodies.length - 1)];
            byte[] b = chosen.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, b.length == 0 ? -1 : b.length);
            if (b.length > 0) {
                exchange.getResponseBody().write(b);
            }
            exchange.close();
        });
        return calls;
    }

    private AiServiceTokenProvider provider(String uri, String clientId, String secret,
                                            String scope, long marginSeconds) {
        return new AiServiceTokenProvider(uri, clientId, secret, scope, marginSeconds, 1000, 2000);
    }

    @Test
    @DisplayName("émet bien une requête client_credentials et renvoie le jeton")
    void obtientUnJeton() {
        AtomicReference<String> form = new AtomicReference<>();
        tokenEndpoint(200, form, "{\"access_token\":\"jeton-1\",\"expires_in\":300}");

        assertThat(provider(tokenUri, "api-quality-engine-ai", "s3cr3t", "", 30).getToken())
                .isEqualTo("jeton-1");
        assertThat(form.get())
                .contains("grant_type=client_credentials")
                .contains("client_id=api-quality-engine-ai")
                .contains("client_secret=s3cr3t");
    }

    @Test
    @DisplayName("le scope n'est transmis que s'il est renseigné")
    void scopeOptionnel() {
        AtomicReference<String> form = new AtomicReference<>();
        tokenEndpoint(200, form, "{\"access_token\":\"j\",\"expires_in\":300}");

        provider(tokenUri, "c", "s", "", 30).getToken();
        assertThat(form.get()).doesNotContain("scope=");

        provider(tokenUri, "c", "s", "ai.invoke", 30).getToken();
        assertThat(form.get()).contains("scope=ai.invoke");
    }

    @Test
    @DisplayName("réutilise le jeton en cache tant qu'il reste valide")
    void reutiliseLeCache() {
        AtomicInteger calls = tokenEndpoint(200, null,
                "{\"access_token\":\"jeton-cache\",\"expires_in\":300}");

        AiServiceTokenProvider p = provider(tokenUri, "c", "s", "", 30);
        assertThat(p.getToken()).isEqualTo("jeton-cache");
        assertThat(p.getToken()).isEqualTo("jeton-cache");

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("redemande un jeton quand la marge couvre déjà l'expiration")
    void rafraichitQuandLaMargeCouvreLExpiration() {
        // expires_in (10 s) inférieur à la marge (30 s) : le jeton n'est jamais
        // réutilisable. C'est voulu — mieux vaut redemander que présenter un jeton
        // sur le point d'expirer, qui serait rejeté en plein appel.
        AtomicInteger calls = tokenEndpoint(200, null,
                "{\"access_token\":\"court-1\",\"expires_in\":10}",
                "{\"access_token\":\"court-2\",\"expires_in\":10}");

        AiServiceTokenProvider p = provider(tokenUri, "c", "s", "", 30);
        assertThat(p.getToken()).isEqualTo("court-1");
        assertThat(p.getToken()).isEqualTo("court-2");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    @DisplayName("configuration incomplète : refuse d'appeler au lieu de se dégrader")
    void configurationIncompleteEchoue() {
        AtomicInteger calls = tokenEndpoint(200, null, "{\"access_token\":\"x\",\"expires_in\":300}");

        assertThatThrownBy(() -> provider("", "c", "s", "", 30).getToken())
                .isInstanceOf(AiGatewayException.class)
                .hasMessageContaining("token-uri");
        assertThatThrownBy(() -> provider(tokenUri, "", "s", "", 30).getToken())
                .isInstanceOf(AiGatewayException.class);
        assertThatThrownBy(() -> provider(tokenUri, "c", "", "", 30).getToken())
                .isInstanceOf(AiGatewayException.class);

        // Aucun appel n'est parti : le refus est immédiat, pas un échec au retour.
        assertThat(calls.get()).isZero();
    }

    @Test
    @DisplayName("réponse sans access_token exploitable : échoue")
    void reponseInexploitable() {
        tokenEndpoint(200, null, "{\"expires_in\":300}");
        assertThatThrownBy(() -> provider(tokenUri, "c", "s", "", 30).getToken())
                .isInstanceOf(AiGatewayException.class)
                .hasMessageContaining("access_token");
    }

    @Test
    @DisplayName("jeton vide : échoue plutôt que d'envoyer un en-tête vide")
    void jetonVide() {
        tokenEndpoint(200, null, "{\"access_token\":\"\",\"expires_in\":300}");
        assertThatThrownBy(() -> provider(tokenUri, "c", "s", "", 30).getToken())
                .isInstanceOf(AiGatewayException.class);
    }

    @Test
    @DisplayName("point d'émission en erreur : échoue sans divulguer le secret")
    void pointDEmissionEnErreur() {
        tokenEndpoint(401, null, "{\"error\":\"invalid_client\"}");
        assertThatThrownBy(() ->
                provider(tokenUri, "c", "un-secret-tres-identifiable", "", 30).getToken())
                .isInstanceOf(AiGatewayException.class)
                .hasMessageNotContaining("un-secret-tres-identifiable");
    }

    @Test
    @DisplayName("expires_in absent : aucune validité supposée, on redemande")
    void expiresInAbsent() {
        AtomicInteger calls = tokenEndpoint(200, null,
                "{\"access_token\":\"sans-duree-1\"}",
                "{\"access_token\":\"sans-duree-2\"}");

        AiServiceTokenProvider p = provider(tokenUri, "c", "s", "", 30);
        assertThat(p.getToken()).isEqualTo("sans-duree-1");
        assertThat(p.getToken()).isEqualTo("sans-duree-2");
        assertThat(calls.get()).isEqualTo(2);
    }
}
