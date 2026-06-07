package com.openlab.qualitos.quality.commconnector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Socle commun aux clients incoming-webhook (Teams / Slack / Mattermost) : un POST JSON
 * vers l'URL fournie, avec timeouts courts (5s connect / 10s read — la lenteur d'un chat
 * tiers ne doit pas pénaliser l'API) et un mapping d'échec uniforme vers
 * {@link CommSendException}.
 *
 * <p>Le {@link RestClient} est injecté pour pouvoir être pointé vers un serveur local
 * dans les tests (même approche que {@code AiGatewayClient}). On NE logue jamais l'URL
 * webhook (elle porte le jeton d'accès au canal).
 */
abstract class AbstractWebhookCommClient implements CommProviderClient {

    private static final Logger log = LoggerFactory.getLogger(AbstractWebhookCommClient.class);

    protected final ObjectMapper mapper;
    private final RestClient client;

    protected AbstractWebhookCommClient(ObjectMapper mapper, RestClient client) {
        this.mapper = mapper;
        this.client = client;
    }

    /** RestClient par défaut (prod) avec timeouts courts. */
    protected static RestClient defaultClient() {
        SimpleClientHttpRequestFactory rf = new SimpleClientHttpRequestFactory();
        rf.setConnectTimeout(5000);
        rf.setReadTimeout(10000);
        return RestClient.builder().requestFactory(rf).build();
    }

    /** Construit le corps JSON spécifique au provider. */
    protected abstract Object buildPayload(CommConnection connection, CommMessage message);

    @Override
    public void send(CommConnection connection, String webhookUrl, CommMessage message) {
        final String body;
        try {
            body = mapper.writeValueAsString(buildPayload(connection, message));
        } catch (JsonProcessingException e) {
            throw new CommSendException(provider() + " payload serialization failed", e);
        }
        try {
            client.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            // HTTP non-2xx renvoyé par le chat (URL révoquée, payload rejeté…).
            log.debug("{} webhook returned HTTP {}", provider(), e.getStatusCode().value());
            throw new CommSendException(provider() + " returned HTTP " + e.getStatusCode().value(), e);
        } catch (Exception e) {
            // Réseau / timeout / DNS — on ne logue jamais l'URL.
            log.debug("{} webhook dispatch error: {}", provider(), e.getMessage());
            throw new CommSendException(provider() + " dispatch failed", e);
        }
    }
}
