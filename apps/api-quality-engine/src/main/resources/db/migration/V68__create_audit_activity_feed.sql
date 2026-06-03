-- V68 : Read-model "flux d'activité" — 1er consommateur Kafka (CLAUDE.md §10.1).
--
-- Projection alimentée par AuditActivityFeedConsumer (@KafkaListener sur le topic
-- qualitos.audit-events, off par défaut) : chaque événement d'audit relayé y est
-- projeté pour des vues "activité récente" par tenant, SANS solliciter la table
-- audit_events (chaîne d'intégrité) à chaque lecture de dashboard.
--
-- Idempotent : unicité (tenant_id, sequence_no) ; la livraison Kafka at-least-once
-- peut rejouer un message → on insère une seule fois (le consumer vérifie l'existence).

CREATE TABLE audit_activity_feed (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    sequence_no     BIGINT NOT NULL,
    occurred_at     TIMESTAMP WITH TIME ZONE,
    recorded_at     TIMESTAMP WITH TIME ZONE,
    action          VARCHAR(100) NOT NULL,
    resource_type   VARCHAR(64),
    resource_id     UUID,
    actor_user_id   UUID,
    summary         VARCHAR(500),
    CONSTRAINT uk_activity_tenant_seq UNIQUE (tenant_id, sequence_no)
);

CREATE INDEX idx_activity_tenant_seq ON audit_activity_feed (tenant_id, sequence_no DESC);

COMMENT ON TABLE audit_activity_feed IS
    'Projection read-model du flux d''audit (consommateur Kafka qualitos.audit-events).';
