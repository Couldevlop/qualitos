package com.openlab.qualitos.quality.commconnector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Consommateur Kafka qui notifie les canaux de communication (Teams/Slack/Mattermost) à
 * partir du flux d'audit {@code qualitos.audit-events} — exactement le même flux que les
 * webhooks sortants ({@code WebhookKafkaConsumer}). On NE réinvente PAS la diffusion : on
 * ajoute simplement un 3e consommateur, dans un <b>groupe distinct</b>
 * ({@code qualitos-comm}), pour recevoir l'intégralité du flux indépendamment des autres.
 *
 * <p><b>Non-invasif & OFF par défaut</b> : actif uniquement si
 * {@code qualitos.kafka.enabled=true} (sinon ce bean n'existe pas, aucun listener, aucun
 * broker requis). De plus, {@link CommConnectorService#notify} ne fait rien tant qu'aucune
 * connexion ACTIVE n'existe — donc même Kafka activé, aucun appel sortant sans config.
 *
 * <p><b>Ciblage</b> : seules les actions d'audit correspondant à un {@link CommEvent.Kind}
 * (NC détectée, CAPA en retard, seuil KPI franchi) déclenchent une notification ; les
 * autres sont ignorées silencieusement. Un message illisible est loggé et acquitté (pas
 * de poison-pill). Le tenant provient de l'enveloppe (jamais d'un body de requête).
 */
@Component
@ConditionalOnProperty(name = "qualitos.kafka.enabled", havingValue = "true")
public class CommKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(CommKafkaConsumer.class);

    private final CommConnectorService commService;
    private final ObjectMapper json;

    public CommKafkaConsumer(CommConnectorService commService, ObjectMapper json) {
        this.commService = commService;
        this.json = json;
    }

    @KafkaListener(
            topics = "${qualitos.kafka.topic:qualitos.audit-events}",
            groupId = "${qualitos.kafka.comm-consumer-group:qualitos-comm}")
    public void consume(String payload) {
        final JsonNode n;
        try {
            n = json.readTree(payload);
        } catch (Exception e) {
            log.warn("[comm] message Kafka illisible, ignoré : {}", e.getMessage());
            return; // acquitté → pas de redélivrance en boucle (poison-pill)
        }

        UUID tenantId = uuid(n, "tenantId");
        String action = text(n, "action");
        if (tenantId == null || action == null) {
            log.warn("[comm] enveloppe incomplète (tenantId/action), ignorée");
            return;
        }

        CommEvent.Kind kind = CommEvent.Kind.fromWire(action);
        if (kind == null) {
            return; // action non ciblée par la notification comm — ignorée silencieusement
        }

        CommEvent event = new CommEvent(
                kind,
                kind.defaultTitle(),
                text(n, "summary"),
                text(n, "resourceType"),
                text(n, "resourceId"),
                kind.defaultSeverity());

        // notify() est best-effort et non bloquant : il ne lève pas, et ne fait rien
        // s'il n'existe aucune connexion ACTIVE pour ce tenant.
        commService.notify(tenantId, event);
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static UUID uuid(JsonNode n, String field) {
        String s = text(n, field);
        if (s == null || s.isBlank()) return null;
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
