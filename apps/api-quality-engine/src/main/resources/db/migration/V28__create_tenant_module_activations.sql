-- V28: Tenant module activations (§10.4)

CREATE TABLE tenant_module_activations (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    module_code         VARCHAR(64) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    billing_tier        VARCHAR(32) NOT NULL,
    configuration_json  TEXT,
    trial_ends_at       TIMESTAMP WITH TIME ZONE,
    expires_at          TIMESTAMP WITH TIME ZONE,
    activated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    activated_by        UUID NOT NULL,
    status_changed_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    last_changed_by     UUID NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_tma_status CHECK (status IN
        ('TRIAL','ACTIVE','SUSPENDED','EXPIRED','DISABLED')),
    CONSTRAINT chk_tma_tier CHECK (billing_tier IN
        ('FREE','STANDARD','PRO','ENTERPRISE')),
    CONSTRAINT chk_tma_module_code CHECK (module_code ~ '^[a-z][a-z0-9-]{1,49}$')
);

CREATE INDEX idx_tma_tenant         ON tenant_module_activations(tenant_id);
CREATE INDEX idx_tma_tenant_code    ON tenant_module_activations(tenant_id, module_code);
CREATE INDEX idx_tma_tenant_status  ON tenant_module_activations(tenant_id, status);
CREATE INDEX idx_tma_due            ON tenant_module_activations(trial_ends_at, expires_at);

-- Unicité applicative : au plus 1 activation non-terminale par (tenant, module).
-- Implémentée comme index partiel pour permettre un historique de désactivations.
CREATE UNIQUE INDEX uk_tma_open_per_tenant_module
    ON tenant_module_activations(tenant_id, module_code)
    WHERE status NOT IN ('EXPIRED', 'DISABLED');

COMMENT ON TABLE tenant_module_activations IS
    'Activations modules par tenant (§10.4) — TRIAL/ACTIVE/SUSPENDED/EXPIRED/DISABLED.';
