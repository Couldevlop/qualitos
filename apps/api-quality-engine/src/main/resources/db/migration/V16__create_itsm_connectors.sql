-- V16: ITSM connectors (CLAUDE.md §13.3)
--
-- Stocke les connexions ITSM par tenant (ServiceNow, Jira Service Management)
-- ainsi que les mappings (incident externe -> entité interne) pour l'idempotence
-- des imports.

CREATE TABLE itsm_connections (
    id                    UUID PRIMARY KEY,
    tenant_id             UUID NOT NULL,
    name                  VARCHAR(120) NOT NULL,
    provider              VARCHAR(32)  NOT NULL,
    base_url              VARCHAR(512) NOT NULL,
    username              VARCHAR(200),
    credential_cipher     VARCHAR(2048) NOT NULL,
    external_scope        VARCHAR(200),
    status                VARCHAR(32) NOT NULL,
    consecutive_failures  INT NOT NULL DEFAULT 0,
    last_sync_at          TIMESTAMP WITH TIME ZONE,
    last_success_at       TIMESTAMP WITH TIME ZONE,
    created_by            UUID NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_itsm_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT chk_itsm_provider CHECK (provider IN ('SERVICENOW', 'JIRA_SM')),
    CONSTRAINT chk_itsm_status   CHECK (status IN ('ACTIVE', 'DISABLED', 'DISABLED_ON_ERRORS')),
    CONSTRAINT chk_itsm_base_url CHECK (base_url LIKE 'https://%')
);

CREATE INDEX idx_itsm_conn_tenant ON itsm_connections(tenant_id);
CREATE INDEX idx_itsm_conn_status ON itsm_connections(tenant_id, status);

CREATE TABLE itsm_incident_mappings (
    id                    UUID PRIMARY KEY,
    tenant_id             UUID NOT NULL,
    connection_id         UUID NOT NULL,
    external_id           VARCHAR(128) NOT NULL,
    external_url          VARCHAR(1024),
    external_status       VARCHAR(64),
    external_priority     VARCHAR(32),
    external_title        VARCHAR(500),
    internal_entity_type  VARCHAR(64) NOT NULL,
    internal_entity_id    UUID,
    first_imported_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    last_seen_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_itsm_map_external UNIQUE (connection_id, external_id),
    CONSTRAINT fk_itsm_map_connection FOREIGN KEY (connection_id)
        REFERENCES itsm_connections(id) ON DELETE CASCADE
);

CREATE INDEX idx_itsm_map_tenant ON itsm_incident_mappings(tenant_id);
CREATE INDEX idx_itsm_map_conn   ON itsm_incident_mappings(connection_id);

COMMENT ON TABLE  itsm_connections IS 'Connexions ITSM par tenant (CLAUDE.md §13.3).';
COMMENT ON COLUMN itsm_connections.credential_cipher IS 'Ciphertext AES-256-GCM base64 (SecretCipher). Vault à terme.';
COMMENT ON TABLE  itsm_incident_mappings IS 'Mapping idempotent incident externe -> entité interne QualitOS.';
