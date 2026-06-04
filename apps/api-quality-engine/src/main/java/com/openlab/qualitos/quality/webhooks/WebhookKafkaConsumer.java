package com.openlab.qualitos.quality.webhooks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openlab.qualitos.quality.common.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 2e consommateur Kafka (CLAUDE.md §10.1, §13.2) : déclenche les webhooks sortants à
 * partir du flux d'audit du topic {@code qualitos.audit-events}. Chaque action d'audit
 * dont le {@code action} correspond à un {@link EventType} souscriptible est publiée via
 * {@link WebhookService#publish(EventType, Map)} pour le tenant concerné.
 *
 * <p><b>Non-invasif</b> : actif uniquement si {@code qualitos.kafka.enabled=true}
 * (OFF par défaut → ce bean n'existe pas, aucun {@code @KafkaListener} démarré, aucun
 * broker requis). Il utilise un <b>groupe consumer distinct</b>
 * ({@code qualitos-webhooks}) de l'activity-feed pour que les deux consommateurs
 * reçoivent chacun l'intégralité du flux.
 *
 * <p><b>Idempotence</b> : assurée en aval — {@link WebhookService} crée une livraison
 * par souscription avec un {@code eventId} propre, et la livraison HTTP est elle-même
 * rejouable. Un message <b>malformé</b> est loggé et acquitté (pas de poison-pill :
 * on NE rejette PAS pour éviter une boucle de redélivrance infinie). Une {@code action}
 * d'audit qui n'est pas un type d'événement souscriptible est <b>ignorée silencieusement</b>.
 * Format d'enveloppe = celui produit par {@code AuditEventKafkaRelay}.
 */
@Component
@ConditionalOnProperty(name = "qualitos.kafka.enabled", havingValue = "true")
public class WebhookKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(WebhookKafkaConsumer.class);

    private final WebhookService webhookService;
    private final ObjectMapper json;

    public WebhookKafkaConsumer(WebhookService webhookService, ObjectMapper json) {
        this.webhookService = webhookService;
        this.json = json;
    }

    @KafkaListener(
            topics = "${qualitos.kafka.topic:qualitos.audit-events}",
            groupId = "${qualitos.kafka.webhooks-consumer-group:qualitos-webhooks}")
    public void consume(String payload) {
        final JsonNode n;
        try {
            n = json.readTree(payload);
        } catch (Exception e) {
            log.warn("[webhooks] message Kafka illisible, ignoré : {}", e.getMessage());
            return; // acquitté → pas de redélivrance en boucle (poison-pill)
        }

        UUID tenantId = uuid(n, "tenantId");
        String action = text(n, "action");
        if (tenantId == null || action == null) {
            log.warn("[webhooks] enveloppe incomplète (tenantId/action), ignorée");
            return;
        }

        // L'action d'audit n'est pas forcément un type d'événement souscriptible :
        // dans ce cas on ignore silencieusement (ce n'est pas un poison-pill).
        EventType type;
        try {
            type = EventType.fromWire(action);
        } catch (IllegalArgumentException e) {
            return;
        }

        // Propagation du tenant : WebhookService.publish() résout le tenant via
        // TenantContext, on le positionne donc AVANT l'appel et on nettoie TOUJOURS.
        TenantContext.setTenantId(tenantId.toString());
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("action", action);
            data.put("resourceType", text(n, "resourceType"));
            data.put("resourceId", text(n, "resourceId"));
            data.put("summary", text(n, "summary"));
            data.put("sequenceNo", n.hasNonNull("sequenceNo") ? n.get("sequenceNo").asLong() : null);
            data.put("occurredAt", text(n, "occurredAt"));
            webhookService.publish(type, data);
        } finally {
            TenantContext.clear();
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static UUID uuid(JsonNode n, String field) {
        String s = text(n, field);
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
