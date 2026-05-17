-- P5 — Custom drag&drop dashboards (CLAUDE.md §7.1, §7.3).
-- Each layout belongs to one user but can be shared across the tenant.

CREATE TABLE dashboard_layouts (
    id                UUID         NOT NULL,
    tenant_id         UUID         NOT NULL,
    user_id           UUID         NOT NULL,
    name              VARCHAR(120) NOT NULL,
    description       VARCHAR(2000),
    layout_json       JSONB        NOT NULL,
    shared            BOOLEAN      NOT NULL DEFAULT FALSE,
    signature_hash    VARCHAR(128),   -- placeholder ML-DSA + blockchain anchor (CLAUDE.md A08)
    version           INTEGER      NOT NULL DEFAULT 1,
    created_at        TIMESTAMPTZ  NOT NULL,
    updated_at        TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_dashboard_layouts PRIMARY KEY (id),
    CONSTRAINT uq_dl_tenant_user_name UNIQUE (tenant_id, user_id, name),
    CONSTRAINT chk_dl_name CHECK (length(trim(name)) >= 2),
    CONSTRAINT chk_dl_layout_object CHECK (jsonb_typeof(layout_json) = 'object')
);

CREATE INDEX idx_dl_tenant         ON dashboard_layouts (tenant_id);
CREATE INDEX idx_dl_tenant_user    ON dashboard_layouts (tenant_id, user_id);
CREATE INDEX idx_dl_tenant_shared  ON dashboard_layouts (tenant_id) WHERE shared = TRUE;

COMMENT ON TABLE  dashboard_layouts IS
    'User-built drag&drop dashboards (CLAUDE.md P5 §7.1, §7.3).';
COMMENT ON COLUMN dashboard_layouts.signature_hash IS
    'ML-DSA signature hash; mirrored on Hyperledger Fabric for integrity (A08).';
