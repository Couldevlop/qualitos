package com.openlab.qualitos.quality.commconnector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Teste les 3 clients incoming-webhook contre un serveur HTTP local (JDK) : capture le
 * corps JSON envoyé pour vérifier le formatage par provider, et couvre succès / 4xx / 5xx.
 */
class CommWebhookClientTest {

    private HttpServer server;
    private int port;
    private final ObjectMapper mapper = new ObjectMapper();
    private final AtomicReference<String> captured = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() { server.stop(0); }

    private RestClient localClient() {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(2000);
        rf.setReadTimeout(5000);
        return RestClient.builder().requestFactory(rf).build();
    }

    private String url() { return "http://localhost:" + port + "/hook"; }

    /** Serveur renvoyant {@code status}, capturant le corps reçu. */
    private void respondWith(int status) {
        server.createContext("/hook", exchange -> {
            captured.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
    }

    private CommConnection conn(CommProvider p, String channel) {
        CommConnection c = new CommConnection();
        c.setId(UUID.randomUUID());
        c.setTenantId(UUID.randomUUID());
        c.setProvider(p);
        c.setName("chan");
        c.setChannel(channel);
        return c;
    }

    private CommMessage message() {
        return new CommMessage("Non-conformité détectée", "Un défaut a été relevé.",
                CommSeverity.CRITICAL, "Ouvrir dans QualitOS", "/app/non-conformity/42",
                List.of(new AbstractMap.SimpleEntry<>("Sévérité", "CRITICAL"),
                        new AbstractMap.SimpleEntry<>("Ressource", "NON_CONFORMITY")));
    }

    // ---- Teams ----

    @Test
    void teams_buildsMessageCardWithThemeColorAndAction() throws IOException {
        respondWith(200);
        new TeamsWebhookClient(mapper, localClient()).send(conn(CommProvider.TEAMS, null), url(), message());

        JsonNode body = mapper.readTree(captured.get());
        assertThat(body.get("@type").asText()).isEqualTo("MessageCard");
        assertThat(body.get("themeColor").asText()).isEqualTo("D0021B"); // CRITICAL hex
        assertThat(body.get("sections").get(0).get("facts")).hasSize(2);
        assertThat(body.get("potentialAction").get(0).get("@type").asText()).isEqualTo("OpenUri");
        assertThat(body.get("potentialAction").get(0).get("targets").get(0).get("uri").asText())
                .isEqualTo("/app/non-conformity/42");
    }

    @Test
    void teams_provider() {
        assertThat(new TeamsWebhookClient(mapper).provider()).isEqualTo(CommProvider.TEAMS);
    }

    // ---- Slack ----

    @Test
    void slack_buildsBlocksAttachmentColorAndChannel() throws IOException {
        respondWith(200);
        new SlackWebhookClient(mapper, localClient()).send(conn(CommProvider.SLACK, "#qualite"), url(), message());

        JsonNode body = mapper.readTree(captured.get());
        assertThat(body.get("channel").asText()).isEqualTo("#qualite");
        assertThat(body.get("text").asText()).contains("Non-conformité détectée");
        JsonNode att = body.get("attachments").get(0);
        assertThat(att.get("color").asText()).isEqualTo("#D0021B");
        assertThat(att.get("blocks").get(0).get("type").asText()).isEqualTo("header");
        // dernier block = actions avec bouton URL
        JsonNode blocks = att.get("blocks");
        JsonNode actions = blocks.get(blocks.size() - 1);
        assertThat(actions.get("type").asText()).isEqualTo("actions");
        assertThat(actions.get("elements").get(0).get("url").asText()).isEqualTo("/app/non-conformity/42");
    }

    @Test
    void slack_provider() {
        assertThat(new SlackWebhookClient(mapper).provider()).isEqualTo(CommProvider.SLACK);
    }

    // ---- Mattermost ----

    @Test
    void mattermost_buildsTextAndAttachmentFields() throws IOException {
        respondWith(200);
        new MattermostWebhookClient(mapper, localClient())
                .send(conn(CommProvider.MATTERMOST, null), url(), message());

        JsonNode body = mapper.readTree(captured.get());
        assertThat(body.get("text").asText()).contains("Non-conformité détectée");
        JsonNode att = body.get("attachments").get(0);
        assertThat(att.get("color").asText()).isEqualTo("#D0021B");
        assertThat(att.get("title_link").asText()).isEqualTo("/app/non-conformity/42");
        assertThat(att.get("fields")).hasSize(2);
        assertThat(att.get("fields").get(0).get("short").asBoolean()).isTrue();
    }

    @Test
    void mattermost_provider() {
        assertThat(new MattermostWebhookClient(mapper).provider()).isEqualTo(CommProvider.MATTERMOST);
    }

    // ---- failure mapping (shared base) ----

    @Test
    void send_http4xx_throwsCommSendException() {
        respondWith(400);
        assertThatThrownBy(() ->
                new SlackWebhookClient(mapper, localClient()).send(conn(CommProvider.SLACK, null), url(), message()))
                .isInstanceOf(CommSendException.class)
                .hasMessageContaining("400");
    }

    @Test
    void send_http5xx_throwsCommSendException() {
        respondWith(503);
        assertThatThrownBy(() ->
                new TeamsWebhookClient(mapper, localClient()).send(conn(CommProvider.TEAMS, null), url(), message()))
                .isInstanceOf(CommSendException.class)
                .hasMessageContaining("503");
    }

    @Test
    void send_networkError_throwsCommSendException() {
        // pas de contexte enregistré + port fermé après stop → connexion refusée
        server.stop(0);
        assertThatThrownBy(() ->
                new MattermostWebhookClient(mapper, localClient())
                        .send(conn(CommProvider.MATTERMOST, null), url(), message()))
                .isInstanceOf(CommSendException.class);
    }

    @Test
    void send_noLink_omitsAction() throws IOException {
        respondWith(200);
        CommMessage noLink = new CommMessage("Info", "corps", CommSeverity.INFO, null, null, List.of());
        new TeamsWebhookClient(mapper, localClient()).send(conn(CommProvider.TEAMS, null), url(), noLink);
        JsonNode body = mapper.readTree(captured.get());
        assertThat(body.has("potentialAction")).isFalse();
    }
}
