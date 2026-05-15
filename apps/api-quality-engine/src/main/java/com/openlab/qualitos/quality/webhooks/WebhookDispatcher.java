package com.openlab.qualitos.quality.webhooks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Dispatcher HTTP des webhooks sortants : POST JSON + signature HMAC.
 *
 * Timeouts courts (5s connect, 10s read) — la lenteur d'un consumer ne doit
 * pas affecter les requetes API en cours.
 *
 * Resultat encapsule dans un record. La logique de re-essai / backoff est
 * gerée plus haut dans {@link WebhookService}.
 */
@Component
public class WebhookDispatcher {

    private static final Logger log = LoggerFactory.getLogger(WebhookDispatcher.class);

    private final HmacSigner signer;
    private final HttpClient httpClient;

    public WebhookDispatcher(HmacSigner signer) {
        this.signer = signer;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    // Constructeur expose pour tests : permet d'injecter un HttpClient mocke.
    WebhookDispatcher(HmacSigner signer, HttpClient httpClient) {
        this.signer = signer;
        this.httpClient = httpClient;
    }

    public DispatchResult dispatch(WebhookSubscription subscription,
                                   EventType eventType,
                                   String eventId,
                                   String jsonPayload) {
        long ts = System.currentTimeMillis();
        String signature = signer.sign(subscription.getSecret(), ts, jsonPayload);

        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(subscription.getEndpointUrl()))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "QualitOS-Webhook/1.0")
                    .header("X-QualitOS-Event", eventType.wire())
                    .header("X-QualitOS-Event-Id", eventId)
                    .header("X-QualitOS-Timestamp", String.valueOf(ts))
                    .header("X-QualitOS-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            int code = res.statusCode();
            boolean ok = code >= 200 && code < 300;
            String body = truncate(res.body(), 4000);
            return new DispatchResult(ok, code, body, null);
        } catch (Exception e) {
            log.debug("Webhook dispatch error for {}: {}", subscription.getEndpointUrl(), e.getMessage());
            return new DispatchResult(false, null, null, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "…(truncated)";
    }

    public record DispatchResult(
            boolean success,
            Integer statusCode,
            String responseBody,
            String errorMessage
    ) {}
}
