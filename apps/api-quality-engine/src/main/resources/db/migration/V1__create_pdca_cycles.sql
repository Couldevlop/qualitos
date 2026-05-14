CREATE TABLE pdca_cycles (
    id          UUID        NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    status      VARCHAR(20) NOT NULL,
    owner_id    UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,

    CONSTRAINT pk_pdca_cycles PRIMARY KEY (id),
    CONSTRAINT chk_pdca_cycles_status CHECK (
        status IN ('PLAN', 'DO', 'CHECK', 'ACT', 'COMPLETED', 'CANCELLED')
    )
);

CREATE INDEX idx_pdca_cycles_tenant_id ON pdca_cycles (tenant_id);
CREATE INDEX idx_pdca_cycles_tenant_status ON pdca_cycles (tenant_id, status);
