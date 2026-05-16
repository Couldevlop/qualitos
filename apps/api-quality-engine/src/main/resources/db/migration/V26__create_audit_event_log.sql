-- V26: Audit event log (foundation for blockchain anchoring — CLAUDE.md §11.5)

CREATE TABLE audit_event_counters (
    tenant_id          UUID PRIMARY KEY,
    last_sequence_no   BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_audit_counter_positive CHECK (last_sequence_no >= 0)
);

CREATE TABLE audit_events (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    sequence_no         BIGINT NOT NULL,
    occurred_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    actor_type          VARCHAR(32) NOT NULL,
    actor_user_id       UUID,
    action              VARCHAR(100) NOT NULL,
    resource_type       VARCHAR(64) NOT NULL,
    resource_id         UUID,
    summary             VARCHAR(500),
    payload_json        TEXT,
    ip_address          VARCHAR(64),
    user_agent          VARCHAR(500),
    integrity_hash      CHAR(64) NOT NULL,
    previous_hash       CHAR(64),
    blockchain_tx_ref   VARCHAR(200),
    CONSTRAINT uk_audit_event_tenant_seq UNIQUE (tenant_id, sequence_no),
    CONSTRAINT uk_audit_event_hash UNIQUE (integrity_hash),
    CONSTRAINT chk_audit_event_actor_type CHECK (actor_type IN
        ('USER','SYSTEM','EXTERNAL','SCHEDULER')),
    CONSTRAINT chk_audit_event_sequence_positive CHECK (sequence_no > 0),
    CONSTRAINT chk_audit_event_action CHECK (action ~ '^[a-z][a-z0-9._-]{1,99}$'),
    CONSTRAINT chk_audit_event_resource CHECK (resource_type ~ '^[a-z][a-z0-9_-]{1,63}$')
);

CREATE INDEX idx_audit_event_tenant_seq       ON audit_events(tenant_id, sequence_no);
CREATE INDEX idx_audit_event_tenant_occurred  ON audit_events(tenant_id, occurred_at);
CREATE INDEX idx_audit_event_tenant_resource  ON audit_events(tenant_id, resource_type, resource_id);
CREATE INDEX idx_audit_event_tenant_actor     ON audit_events(tenant_id, actor_user_id);
CREATE INDEX idx_audit_event_tenant_action    ON audit_events(tenant_id, action);

COMMENT ON TABLE audit_events IS
    'Journal d''audit append-only chaîné SHA-256 (§11.5).';
COMMENT ON COLUMN audit_events.integrity_hash IS
    'SHA-256 hex de la ligne courante chaînée à previous_hash.';
COMMENT ON COLUMN audit_events.blockchain_tx_ref IS
    'Référence transaction Hyperledger Fabric une fois ancré.';
COMMENT ON TABLE audit_event_counters IS
    'Compteur monotonique par tenant — accédé en PESSIMISTIC_WRITE.';
