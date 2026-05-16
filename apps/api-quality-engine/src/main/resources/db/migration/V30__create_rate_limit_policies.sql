-- V30: Rate-limit policies (OWASP A04 Insecure Design / A07 Auth Failures)

CREATE TABLE rate_limit_policies (
    id              UUID PRIMARY KEY,
    tenant_id       UUID NOT NULL,
    scope           VARCHAR(100) NOT NULL,
    window_seconds  INT NOT NULL,
    max_requests    INT NOT NULL,
    enabled         BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_rate_limit_tenant_scope UNIQUE (tenant_id, scope),
    CONSTRAINT chk_rate_limit_window CHECK (window_seconds BETWEEN 1 AND 86400),
    CONSTRAINT chk_rate_limit_max CHECK (max_requests BETWEEN 1 AND 1000000),
    CONSTRAINT chk_rate_limit_scope CHECK (scope ~ '^[a-z][a-z0-9._:-]{0,99}$')
);

CREATE INDEX idx_rate_limit_tenant         ON rate_limit_policies(tenant_id);
CREATE INDEX idx_rate_limit_tenant_enabled ON rate_limit_policies(tenant_id, enabled);

COMMENT ON TABLE rate_limit_policies IS
    'Politiques rate-limit par (tenant, scope) — fenêtre fixe, compteur en mémoire (V1).';
