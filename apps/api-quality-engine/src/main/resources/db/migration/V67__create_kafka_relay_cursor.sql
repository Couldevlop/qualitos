-- V67 : Socle Kafka non-invasif (CLAUDE.md §10.1 event-driven, §13.2 webhooks).
--
-- Curseur du relais "outbox" : pour chaque tenant, dernière séquence d'audit déjà
-- publiée sur Kafka. Le relais (AuditEventKafkaRelay, @ConditionalOnProperty
-- qualitos.kafka.enabled, OFF par défaut) lit les nouveaux audit_events au-delà de ce
-- curseur et les publie sur un topic. On réutilise `audit_events` comme outbox : c'est
-- une table append-only, écrite dans la transaction métier, dont `sequence_no` est
-- monotone et SANS TROU par tenant (compteur verrouillé) → un curseur "> last" ne saute
-- jamais d'événement. Aucun changement du domaine ni des *EventPublisher existants.

CREATE TABLE kafka_relay_cursor (
    tenant_id           UUID PRIMARY KEY,
    last_published_seq  BIGINT NOT NULL DEFAULT 0,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

COMMENT ON TABLE kafka_relay_cursor IS
    'Dernière séquence audit_events publiée sur Kafka par tenant (relais outbox, off par défaut).';
