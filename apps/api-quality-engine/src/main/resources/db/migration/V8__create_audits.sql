CREATE TABLE audit_plans (
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id        UUID         NOT NULL,
    title            VARCHAR(255) NOT NULL,
    scope            TEXT,
    type             VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    standard         VARCHAR(100),
    lead_auditor_id  UUID         NOT NULL,
    auditee_id       UUID,
    scheduled_date   DATE,
    started_at       TIMESTAMPTZ,
    completed_at     TIMESTAMPTZ,
    report_summary   TEXT,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_audit_plans PRIMARY KEY (id),
    CONSTRAINT chk_audit_plans_type CHECK (
        type IN ('INTERNAL','EXTERNAL','SUPPLIER','LPA','CERTIFICATION','SURVEILLANCE')
    ),
    CONSTRAINT chk_audit_plans_status CHECK (
        status IN ('PLANNED','IN_PROGRESS','COMPLETED','CANCELLED')
    )
);

CREATE INDEX idx_audit_plans_tenant ON audit_plans (tenant_id);
CREATE INDEX idx_audit_plans_tenant_status ON audit_plans (tenant_id, status);
CREATE INDEX idx_audit_plans_tenant_type ON audit_plans (tenant_id, type);
CREATE INDEX idx_audit_plans_scheduled ON audit_plans (scheduled_date);

CREATE TABLE audit_checklist_items (
    id                 UUID        NOT NULL DEFAULT gen_random_uuid(),
    plan_id            UUID        NOT NULL,
    question           TEXT        NOT NULL,
    clause_ref         VARCHAR(100),
    expected_evidence  TEXT,
    weight             INTEGER     NOT NULL DEFAULT 1,
    order_index        INTEGER     NOT NULL,
    response           TEXT,
    conformant         BOOLEAN,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_audit_checklist_items PRIMARY KEY (id),
    CONSTRAINT fk_audit_checklist_items_plan FOREIGN KEY (plan_id)
        REFERENCES audit_plans (id) ON DELETE CASCADE,
    CONSTRAINT chk_audit_checklist_weight CHECK (weight >= 1)
);

CREATE INDEX idx_audit_checklist_plan ON audit_checklist_items (plan_id);

CREATE TABLE audit_findings (
    id                 UUID        NOT NULL DEFAULT gen_random_uuid(),
    plan_id            UUID        NOT NULL,
    checklist_item_id  UUID,
    type               VARCHAR(20) NOT NULL,
    description        TEXT        NOT NULL,
    clause_ref         VARCHAR(100),
    photo_url          VARCHAR(1024),
    capa_id            UUID,
    raised_by          UUID        NOT NULL,
    raised_at          TIMESTAMPTZ NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_audit_findings PRIMARY KEY (id),
    CONSTRAINT fk_audit_findings_plan FOREIGN KEY (plan_id)
        REFERENCES audit_plans (id) ON DELETE CASCADE,
    CONSTRAINT fk_audit_findings_item FOREIGN KEY (checklist_item_id)
        REFERENCES audit_checklist_items (id) ON DELETE SET NULL,
    CONSTRAINT chk_audit_findings_type CHECK (
        type IN ('CONFORMITY','MINOR_NC','MAJOR_NC','OBSERVATION','OPPORTUNITY')
    )
);

CREATE INDEX idx_audit_findings_plan ON audit_findings (plan_id);
CREATE INDEX idx_audit_findings_type ON audit_findings (plan_id, type);
CREATE INDEX idx_audit_findings_capa ON audit_findings (capa_id);
