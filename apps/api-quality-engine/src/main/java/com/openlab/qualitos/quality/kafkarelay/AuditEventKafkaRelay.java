package com.openlab.qualitos.quality.kafkarelay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openlab.qualitos.quality.auditlog.AuditEvent;
import com.openlab.qualitos.quality.auditlog.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Relais "outbox" : publie les événements {@code audit_events} sur un topic Kafka,
 * par tenant, au-delà d'un curseur de séquence (CLAUDE.md §10.1 event-driven, §13.2).
 *
 * <p><b>Non-invasif & sans régression</b> : activé uniquement si
 * {@code qualitos.kafka.enabled=true} (OFF par défaut → ce bean n'existe pas, aucun
 * changement de comportement). On réutilise {@code audit_events} comme outbox (append-only,
 * écrit dans la transaction métier, {@code sequence_no} monotone et sans trou par tenant)
 * — donc aucune modification du domaine ni des {@code *EventPublisher} existants, et pas
 * de dual-write : on lit ce qui est DÉJÀ committé.
 *
 * <p><b>Livraison at-least-once</b> : on {@code flush()} les envois avant d'avancer le
 * curseur ; un échec d'envoi laisse le curseur en place → re-tenté au prochain cycle.
 * La clé Kafka = {@code tenantId} (ordre par partition préservé par tenant).
 */
@Component
@ConditionalOnProperty(name = "qualitos.kafka.enabled", havingValue = "true")
public class AuditEventKafkaRelay {

    private static final Logger log = LoggerFactory.getLogger(AuditEventKafkaRelay.class);

    private final AuditEventRepository events;
    private final KafkaRelayCursorRepository cursors;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper json;
    private final String topic;
    private final int batchSize;

    public AuditEventKafkaRelay(
            AuditEventRepository events,
            KafkaRelayCursorRepository cursors,
            KafkaTemplate<String, String> kafka,
            ObjectMapper json,
            @Value("${qualitos.kafka.topic:qualitos.audit-events}") String topic,
            @Value("${qualitos.kafka.batch-size:200}") int batchSize) {
        this.events = events;
        this.cursors = cursors;
        this.kafka = kafka;
        this.json = json;
        this.topic = topic;
        this.batchSize = batchSize;
    }

    @Scheduled(
            initialDelayString = "${qualitos.kafka.initial-delay-ms:30000}",
            fixedDelayString = "${qualitos.kafka.fixed-delay-ms:10000}")
    public void relayPending() {
        for (UUID tenant : events.findDistinctTenantIds()) {
            try {
                relayTenant(tenant);
            } catch (RuntimeException e) {
                // un tenant en échec ne bloque pas les autres ; re-tenté au prochain cycle
                log.warn("[kafka-relay] tenant {} : publication différée ({})", tenant, e.getMessage());
            }
        }
    }

    /** Publie le lot de nouveaux événements d'un tenant puis avance le curseur. */
    @Transactional
    public void relayTenant(UUID tenantId) {
        long cursor = cursors.findById(tenantId)
                .map(KafkaRelayCursor::getLastPublishedSeq)
                .orElse(0L);

        List<AuditEvent> batch = events.findByTenantIdAndSequenceNoGreaterThanOrderBySequenceNoAsc(
                tenantId, cursor, PageRequest.of(0, batchSize));
        if (batch.isEmpty()) {
            return;
        }

        long last = cursor;
        for (AuditEvent e : batch) {
            kafka.send(topic, tenantId.toString(), toEnvelope(e));
            last = e.getSequenceNo();
        }
        kafka.flush(); // garantit l'envoi avant d'avancer le curseur (at-least-once)

        KafkaRelayCursor c = cursors.findById(tenantId)
                .orElseGet(() -> new KafkaRelayCursor(tenantId, 0L, Instant.now()));
        c.setLastPublishedSeq(last);
        c.setUpdatedAt(Instant.now());
        cursors.save(c);

        log.info("[kafka-relay] tenant={} : {} événement(s) publiés (seq {}→{}) sur {}",
                tenantId, batch.size(), cursor, last, topic);
    }

    private String toEnvelope(AuditEvent e) {
        ObjectNode n = json.createObjectNode();
        n.put("id", e.getId() == null ? null : e.getId().toString());
        n.put("tenantId", e.getTenantId().toString());
        n.put("sequenceNo", e.getSequenceNo());
        n.put("occurredAt", e.getOccurredAt() == null ? null : e.getOccurredAt().toString());
        n.put("recordedAt", e.getRecordedAt() == null ? null : e.getRecordedAt().toString());
        n.put("actorType", e.getActorType() == null ? null : e.getActorType().name());
        n.put("actorUserId", e.getActorUserId() == null ? null : e.getActorUserId().toString());
        n.put("action", e.getAction());
        n.put("resourceType", e.getResourceType());
        n.put("resourceId", e.getResourceId() == null ? null : e.getResourceId().toString());
        n.put("summary", e.getSummary());
        // payload_json est déjà du JSON → on l'imbrique en string (le consommateur re-parse)
        n.put("payloadJson", e.getPayloadJson());
        n.put("integrityHash", e.getIntegrityHash());
        return n.toString();
    }
}
