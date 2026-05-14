CREATE TABLE fives_audits (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL,
    zone          VARCHAR(200) NOT NULL,
    description   TEXT,
    status        VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    auditor_id    UUID         NOT NULL,
    scheduled_at  TIMESTAMPTZ,
    completed_at  TIMESTAMPTZ,
    overall_score DOUBLE PRECISION,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_fives_audits PRIMARY KEY (id),
    CONSTRAINT chk_fives_audits_status CHECK (
        status IN ('DRAFT', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')
    ),
    CONSTRAINT chk_fives_audits_score CHECK (
        overall_score IS NULL OR (overall_score >= 0.0 AND overall_score <= 100.0)
    )
);

CREATE INDEX idx_fives_audits_tenant_id ON fives_audits (tenant_id);
CREATE INDEX idx_fives_audits_tenant_status ON fives_audits (tenant_id, status);

CREATE TABLE fives_audit_items (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    audit_id   UUID        NOT NULL,
    pillar     VARCHAR(20) NOT NULL,
    score      INTEGER     NOT NULL,
    note       TEXT,
    photo_url  VARCHAR(1024),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_fives_audit_items PRIMARY KEY (id),
    CONSTRAINT fk_fives_audit_items_audit FOREIGN KEY (audit_id)
        REFERENCES fives_audits (id) ON DELETE CASCADE,
    CONSTRAINT uk_fives_audit_pillar UNIQUE (audit_id, pillar),
    CONSTRAINT chk_fives_audit_items_pillar CHECK (
        pillar IN ('SEIRI', 'SEITON', 'SEISO', 'SEIKETSU', 'SHITSUKE')
    ),
    CONSTRAINT chk_fives_audit_items_score CHECK (score >= 0 AND score <= 10)
);

CREATE INDEX idx_fives_audit_items_audit_id ON fives_audit_items (audit_id);
