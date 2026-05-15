-- Webhooks sortants (CLAUDE.md §13.2).
-- HMAC-SHA256 signature + retry exponentiel + dead-letter.

CREATE TABLE webhook_subscriptions (
    id                     UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id              UUID         NOT NULL,
    name                   VARCHAR(255) NOT NULL,
    endpoint_url           VARCHAR(2048) NOT NULL,
    event_types            TEXT         NOT NULL,
    secret                 VARCHAR(128) NOT NULL,
    status                 VARCHAR(30)  NOT NULL DEFAULT 'ACTIVE',
    max_retries            INTEGER      NOT NULL DEFAULT 5,
    consecutive_failures   INTEGER      NOT NULL DEFAULT 0,
    last_triggered_at      TIMESTAMPTZ,
    last_success_at        TIMESTAMPTZ,
    created_by             UUID         NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_webhook_subscriptions PRIMARY KEY (id),
    CONSTRAINT chk_webhook_status CHECK (status IN ('ACTIVE','PAUSED','DISABLED_ON_ERRORS')),
    CONSTRAINT chk_webhook_max_retries CHECK (max_retries >= 0 AND max_retries <= 10)
);

CREATE INDEX idx_webhook_subs_tenant ON webhook_subscriptions (tenant_id);
CREATE INDEX idx_webhook_subs_tenant_status ON webhook_subscriptions (tenant_id, status);

CREATE TABLE webhook_deliveries (
    id                    UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id             UUID         NOT NULL,
    subscription_id       UUID         NOT NULL,
    event_id              VARCHAR(36)  NOT NULL,
    event_type            VARCHAR(64)  NOT NULL,
    payload               TEXT         NOT NULL,
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    attempt_count         INTEGER      NOT NULL DEFAULT 0,
    last_attempt_at       TIMESTAMPTZ,
    next_retry_at         TIMESTAMPTZ,
    response_status_code  INTEGER,
    response_body         TEXT,
    error_message         TEXT,
    created_at            TIMESTAMPTZ  NOT NULL,
    updated_at            TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_webhook_deliveries PRIMARY KEY (id),
    CONSTRAINT fk_webhook_deliveries_sub FOREIGN KEY (subscription_id)
        REFERENCES webhook_subscriptions (id) ON DELETE CASCADE,
    CONSTRAINT chk_webhook_delivery_status CHECK (
        status IN ('PENDING','SUCCESS','FAILED','RETRYING','DEAD_LETTER')
    )
);

CREATE INDEX idx_webhook_deliveries_tenant ON webhook_deliveries (tenant_id);
CREATE INDEX idx_webhook_deliveries_tenant_status ON webhook_deliveries (tenant_id, status);
CREATE INDEX idx_webhook_deliveries_sub ON webhook_deliveries (subscription_id);
CREATE INDEX idx_webhook_deliveries_status_retry ON webhook_deliveries (status, next_retry_at)
    WHERE status = 'RETRYING';
CREATE INDEX idx_webhook_deliveries_event ON webhook_deliveries (event_id);
