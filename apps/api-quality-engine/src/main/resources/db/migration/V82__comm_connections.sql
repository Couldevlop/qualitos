-- V82: Comm connectors (CLAUDE.md §13.3 « Communication »)
--
-- Connexions de communication sortante par tenant (Teams, Slack, Mattermost).
-- Calquée sur itsm_connections (V16). L'URL d'incoming-webhook EST le secret : elle est
-- chiffrée au repos (SecretCipher, AES-256-GCM base64) dans webhook_url_cipher et n'est
-- jamais stockée en clair. On réutilise les statuts de la couche connecteurs
-- (ACTIVE / DISABLED / DISABLED_ON_ERRORS).

CREATE TABLE comm_connections (
    id                    UUID PRIMARY KEY,
    tenant_id             UUID NOT NULL,
    name                  VARCHAR(120) NOT NULL,
    provider              VARCHAR(32)  NOT NULL,
    webhook_url_cipher    VARCHAR(2048) NOT NULL,
    channel               VARCHAR(200),
    status                VARCHAR(32) NOT NULL,
    consecutive_failures  INT NOT NULL DEFAULT 0,
    last_notified_at      TIMESTAMP WITH TIME ZONE,
    last_success_at       TIMESTAMP WITH TIME ZONE,
    created_by            UUID NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_comm_tenant_name UNIQUE (tenant_id, name),
    CONSTRAINT chk_comm_provider CHECK (provider IN ('TEAMS', 'SLACK', 'MATTERMOST')),
    CONSTRAINT chk_comm_status   CHECK (status IN ('ACTIVE', 'DISABLED', 'DISABLED_ON_ERRORS'))
);

CREATE INDEX idx_comm_conn_tenant ON comm_connections(tenant_id);
CREATE INDEX idx_comm_conn_status ON comm_connections(tenant_id, status);

COMMENT ON TABLE  comm_connections IS 'Connexions de communication sortante par tenant (CLAUDE.md §13.3).';
COMMENT ON COLUMN comm_connections.webhook_url_cipher IS 'Ciphertext AES-256-GCM base64 (SecretCipher) de l''URL incoming-webhook (le secret). Vault à terme.';
