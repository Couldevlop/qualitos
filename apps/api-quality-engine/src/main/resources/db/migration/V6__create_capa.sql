CREATE TABLE capa_cases (
    id                        UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id                 UUID         NOT NULL,
    title                     VARCHAR(255) NOT NULL,
    description               TEXT,
    type                      VARCHAR(20)  NOT NULL,
    criticity                 VARCHAR(20)  NOT NULL,
    status                    VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    source_type               VARCHAR(30)  NOT NULL,
    source_ref                VARCHAR(255),
    owner_id                  UUID         NOT NULL,
    root_cause_id             UUID,
    due_date                  DATE,
    resolved_at               TIMESTAMPTZ,
    closed_at                 TIMESTAMPTZ,
    effectiveness_verified    BOOLEAN,
    effectiveness_verified_at TIMESTAMPTZ,
    created_at                TIMESTAMPTZ  NOT NULL,
    updated_at                TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_capa_cases PRIMARY KEY (id),
    CONSTRAINT chk_capa_cases_type CHECK (type IN ('CORRECTIVE', 'PREVENTIVE')),
    CONSTRAINT chk_capa_cases_criticity CHECK (criticity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_capa_cases_status CHECK (
        status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REJECTED')
    ),
    CONSTRAINT chk_capa_cases_source CHECK (
        source_type IN ('NON_CONFORMITY', 'AUDIT', 'COMPLAINT', 'INTERNAL', 'IOT_ALERT', 'OTHER')
    )
);

CREATE INDEX idx_capa_cases_tenant_id ON capa_cases (tenant_id);
CREATE INDEX idx_capa_cases_tenant_status ON capa_cases (tenant_id, status);
CREATE INDEX idx_capa_cases_owner ON capa_cases (tenant_id, owner_id);
CREATE INDEX idx_capa_cases_due_date ON capa_cases (due_date);

CREATE TABLE capa_actions (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    capa_id      UUID         NOT NULL,
    title        VARCHAR(255) NOT NULL,
    description  TEXT,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    assignee_id  UUID,
    due_date     DATE,
    completed_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_capa_actions PRIMARY KEY (id),
    CONSTRAINT fk_capa_actions_case FOREIGN KEY (capa_id)
        REFERENCES capa_cases (id) ON DELETE CASCADE,
    CONSTRAINT chk_capa_actions_status CHECK (
        status IN ('PENDING', 'IN_PROGRESS', 'DONE')
    )
);

CREATE INDEX idx_capa_actions_capa_id ON capa_actions (capa_id);
CREATE INDEX idx_capa_actions_assignee ON capa_actions (assignee_id);
