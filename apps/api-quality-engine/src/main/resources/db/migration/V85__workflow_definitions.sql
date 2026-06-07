-- Designer de workflow BPMN no-code (§5.4) : persistance des définitions par tenant.
CREATE TABLE workflow_definitions (
    id          UUID         NOT NULL DEFAULT gen_random_uuid(),
    tenant_id   UUID         NOT NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    bpmn_xml    TEXT         NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    version     INTEGER      NOT NULL DEFAULT 1,
    created_by  UUID,
    updated_by  UUID,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,

    CONSTRAINT pk_workflow_definitions PRIMARY KEY (id),
    CONSTRAINT chk_workflow_definitions_status CHECK (
        status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')
    )
);

CREATE INDEX idx_workflow_definitions_tenant ON workflow_definitions (tenant_id);
CREATE INDEX idx_workflow_definitions_tenant_status ON workflow_definitions (tenant_id, status);
CREATE INDEX idx_workflow_definitions_tenant_updated_at ON workflow_definitions (tenant_id, updated_at DESC);
