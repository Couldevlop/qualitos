CREATE TABLE ishikawa_diagrams (
    id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id          UUID         NOT NULL,
    problem_statement  VARCHAR(500) NOT NULL,
    description        TEXT,
    mode               VARCHAR(10)  NOT NULL DEFAULT 'SIX_M',
    status             VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    owner_id           UUID         NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_ishikawa_diagrams PRIMARY KEY (id),
    CONSTRAINT chk_ishikawa_diagrams_mode CHECK (
        mode IN ('SIX_M', 'SEVEN_M', 'EIGHT_M')
    ),
    CONSTRAINT chk_ishikawa_diagrams_status CHECK (
        status IN ('DRAFT', 'IN_REVIEW', 'VALIDATED', 'ARCHIVED')
    )
);

CREATE INDEX idx_ishikawa_diagrams_tenant_id ON ishikawa_diagrams (tenant_id);
CREATE INDEX idx_ishikawa_diagrams_tenant_status ON ishikawa_diagrams (tenant_id, status);
