-- V27: EHS incidents (CLAUDE.md §4.11)

CREATE TABLE ehs_incidents (
    id                  UUID PRIMARY KEY,
    tenant_id           UUID NOT NULL,
    code                VARCHAR(100) NOT NULL,
    title               VARCHAR(250) NOT NULL,
    description         VARCHAR(4000),
    type                VARCHAR(32) NOT NULL,
    severity            VARCHAR(32) NOT NULL,
    status              VARCHAR(32) NOT NULL,
    occurred_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    reported_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    mitigated_at        TIMESTAMP WITH TIME ZONE,
    closed_at           TIMESTAMP WITH TIME ZONE,
    location            VARCHAR(500),
    persons_involved    VARCHAR(1000),
    root_cause          VARCHAR(2000),
    corrective_actions  VARCHAR(2000),
    standards_csv       VARCHAR(500),
    capa_case_id        UUID,
    nc_id               UUID,
    owner_user_id       UUID,
    reported_by         UUID NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_ehs_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT chk_ehs_type CHECK (type IN
        ('INJURY','NEAR_MISS','ENVIRONMENTAL','SECURITY','PROPERTY_DAMAGE','OTHER')),
    CONSTRAINT chk_ehs_severity CHECK (severity IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_ehs_status CHECK (status IN
        ('REPORTED','INVESTIGATING','MITIGATED','CLOSED','CANCELLED'))
);

CREATE INDEX idx_ehs_tenant          ON ehs_incidents(tenant_id);
CREATE INDEX idx_ehs_tenant_status   ON ehs_incidents(tenant_id, status);
CREATE INDEX idx_ehs_tenant_type     ON ehs_incidents(tenant_id, type);
CREATE INDEX idx_ehs_tenant_severity ON ehs_incidents(tenant_id, severity);

COMMENT ON TABLE ehs_incidents IS 'Incidents EHS (§4.11) — pipeline réutilise CAPA + NC via FK molles.';
