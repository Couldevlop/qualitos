package com.openlab.qualitos.quality.activityfeed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * 1er consommateur Kafka (CLAUDE.md §10.1) : projette les événements d'audit du topic
 * {@code qualitos.audit-events} dans le read-model {@link AuditActivityEntry} (flux
 * d'activité par tenant).
 *
 * <p><b>Non-invasif</b> : actif uniquement si {@code qualitos.kafka.enabled=true}
 * (OFF par défaut → ce bean n'existe pas, aucun {@code @KafkaListener} démarré, aucun
 * broker requis). <b>Idempotent</b> : la livraison at-least-once peut rejouer un message,
 * on ne projette qu'une fois par (tenant, sequenceNo). Un message <b>malformé</b> est
 * loggé et acquitté (pas de poison-pill : on NE rejette PAS pour éviter une boucle de
 * redélivrance infinie). Format d'enveloppe = celui produit par {@code AuditEventKafkaRelay}.
 */
@Component
@ConditionalOnProperty(name = "qualitos.kafka.enabled", havingValue = "true")
public class AuditActivityFeedConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditActivityFeedConsumer.class);

    private final AuditActivityRepository repository;
    private final ObjectMapper json;

    public AuditActivityFeedConsumer(AuditActivityRepository repository, ObjectMapper json) {
        this.repository = repository;
        this.json = json;
    }

    @KafkaListener(
            topics = "${qualitos.kafka.topic:qualitos.audit-events}",
            groupId = "${qualitos.kafka.consumer-group:qualitos-activity-feed}")
    @Transactional
    public void consume(String payload) {
        final JsonNode n;
        try {
            n = json.readTree(payload);
        } catch (Exception e) {
            log.warn("[activity-feed] message Kafka illisible, ignoré : {}", e.getMessage());
            return; // acquitté → pas de redélivrance en boucle (poison-pill)
        }

        UUID tenantId = uuid(n, "tenantId");
        String action = text(n, "action");
        if (tenantId == null || action == null || !n.hasNonNull("sequenceNo")) {
            log.warn("[activity-feed] enveloppe incomplète (tenantId/sequenceNo/action), ignorée");
            return;
        }
        long seq = n.get("sequenceNo").asLong();

        if (repository.existsByTenantIdAndSequenceNo(tenantId, seq)) {
            return; // déjà projeté (idempotent)
        }

        UUID id = uuid(n, "id");
        AuditActivityEntry entry = new AuditActivityEntry(
                id != null ? id : UUID.randomUUID(),
                tenantId,
                seq,
                instant(n, "occurredAt"),
                instant(n, "recordedAt"),
                action,
                text(n, "resourceType"),
                uuid(n, "resourceId"),
                uuid(n, "actorUserId"),
                text(n, "summary"));
        repository.save(entry);
        log.debug("[activity-feed] projeté tenant={} seq={} action={}", tenantId, seq, action);
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

    private static Instant instant(JsonNode n, String field) {
        String s = text(n, field);
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(s);
        } catch (Exception e) {
            return null;
        }
    }
}
